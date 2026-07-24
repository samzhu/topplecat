package io.github.samzhu.topplecat.core;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Project-scoped custody lock used by one operation or an entire verification run. */
public final class EscrowProjectLock implements AutoCloseable {
    private static final Path ESCROW_ROOT = Path.of(".topplecat", "escrow");
    private static final Map<Path, HeldLock> VERIFICATION_LOCKS = new HashMap<>();

    private final HeldLock operationLock;

    private EscrowProjectLock(HeldLock operationLock) {
        this.operationLock = operationLock;
    }

    /** Acquires a short-lived lock unless the current verification run already owns it. */
    public static EscrowProjectLock acquireOperation(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        synchronized (VERIFICATION_LOCKS) {
            if (VERIFICATION_LOCKS.containsKey(root)) {
                return new EscrowProjectLock(null);
            }
        }
        return new EscrowProjectLock(acquire(root));
    }

    /** Holds the lock until {@link #releaseVerification(Path)} runs. */
    public static void acquireForVerification(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        synchronized (VERIFICATION_LOCKS) {
            if (VERIFICATION_LOCKS.containsKey(root)) {
                throw custodyBusy();
            }
            VERIFICATION_LOCKS.put(root, acquire(root));
        }
    }

    /** Releases a lock retained for a verification run. Safe when acquisition did not complete. */
    public static void releaseVerification(Path projectRoot) {
        Path root = normalizedRoot(projectRoot);
        HeldLock held;
        synchronized (VERIFICATION_LOCKS) {
            held = VERIFICATION_LOCKS.remove(root);
        }
        if (held != null) {
            held.close();
        }
    }

    @Override
    public void close() {
        if (operationLock != null) {
            operationLock.close();
        }
    }

    private static HeldLock acquire(Path root) {
        Path path = root.resolve(ESCROW_ROOT).resolve(".lock");
        try {
            Files.createDirectories(Objects.requireNonNull(path.getParent()));
            FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            try {
                FileLock lock = channel.tryLock();
                if (lock == null) {
                    channel.close();
                    throw custodyBusy();
                }
                return new HeldLock(channel, lock);
            } catch (OverlappingFileLockException exception) {
                channel.close();
                throw custodyBusy();
            } catch (IOException exception) {
                channel.close();
                throw exception;
            }
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot acquire ToppleCat custody lock " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static Path normalizedRoot(Path projectRoot) {
        return projectRoot.toAbsolutePath().normalize();
    }

    private static ToppleCatException custodyBusy() {
        return new ToppleCatException("Another ToppleCat custody operation is already running for this project. "
                + "Wait for it to finish before hiding, restoring, or verifying reviewer source.");
    }

    private record HeldLock(FileChannel channel, FileLock lock) implements AutoCloseable {
        @Override
        public void close() {
            try {
                lock.release();
                channel.close();
            } catch (IOException exception) {
                throw new ToppleCatException("Cannot release ToppleCat custody lock: " + exception.getMessage(), exception);
            }
        }
    }
}
