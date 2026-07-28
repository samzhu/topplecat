import { createHash } from "node:crypto";
import { access, mkdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const siteRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const verifyOnly = process.argv.slice(2).includes("--verify");
const unsupportedArgs = process.argv.slice(2).filter((argument) => argument !== "--verify");

if (unsupportedArgs.length > 0) {
  throw new Error(`Unsupported argument(s): ${unsupportedArgs.join(", ")}`);
}

const assets = [
  {
    source: "src/assets/original/backgrounds/argyle-tile.org.png",
    dimensions: { width: 976, height: 872 },
    outputs: [
      { destination: "src/assets/backgrounds/argyle-tile.avif", format: "avif", options: { quality: 54, effort: 6 } },
      { destination: "src/assets/backgrounds/argyle-tile.webp", format: "webp", options: { quality: 78, effort: 6 } },
    ],
  },
  {
    source: "src/assets/original/characters/cat-action-sprite.org.png",
    dimensions: { width: 2661, height: 887 },
    outputs: [
      { destination: "src/assets/characters/cat-action-sprite.avif", format: "avif", options: { quality: 62, effort: 6, chromaSubsampling: "4:4:4" } },
      { destination: "src/assets/characters/cat-action-sprite.webp", format: "webp", options: { quality: 82, alphaQuality: 100, effort: 6, smartSubsample: true } },
    ],
  },
  ...["cup-upright", "cup-tipped"].flatMap((name) => [
    {
      source: `src/assets/original/props/${name}.org.png`,
      dimensions: { width: 1254, height: 1254 },
      resizeWidth: 320,
      outputs: [
        { destination: `src/assets/props/${name}-320.avif`, format: "avif", options: { quality: 64, effort: 6, chromaSubsampling: "4:4:4" } },
        { destination: `src/assets/props/${name}-320.webp`, format: "webp", options: { quality: 84, alphaQuality: 100, effort: 6, smartSubsample: true } },
      ],
    },
    {
      source: `src/assets/original/props/${name}.org.png`,
      dimensions: { width: 1254, height: 1254 },
      resizeWidth: 640,
      outputs: [
        { destination: `src/assets/props/${name}-640.avif`, format: "avif", options: { quality: 64, effort: 6, chromaSubsampling: "4:4:4" } },
        { destination: `src/assets/props/${name}-640.webp`, format: "webp", options: { quality: 84, alphaQuality: 100, effort: 6, smartSubsample: true } },
      ],
    },
    {
      source: `src/assets/original/props/${name}.org.png`,
      dimensions: { width: 1254, height: 1254 },
      resizeWidth: 960,
      outputs: [
        { destination: `src/assets/props/${name}-960.avif`, format: "avif", options: { quality: 64, effort: 6, chromaSubsampling: "4:4:4" } },
        { destination: `src/assets/props/${name}-960.webp`, format: "webp", options: { quality: 84, alphaQuality: 100, effort: 6, smartSubsample: true } },
      ],
    },
  ]),
];

const resolve = (relativePath) => path.join(siteRoot, relativePath);
const digest = async (file) => createHash("sha256").update(await readFile(file)).digest("hex");

async function assertMetadata(file, expectedDimensions, expectedAlpha) {
  const metadata = await sharp(file).metadata();
  if (metadata.width !== expectedDimensions.width || metadata.height !== expectedDimensions.height) {
    throw new Error(`${path.relative(siteRoot, file)} is ${metadata.width}×${metadata.height}; expected ${expectedDimensions.width}×${expectedDimensions.height}.`);
  }
  if (metadata.hasAlpha !== expectedAlpha) {
    throw new Error(`${path.relative(siteRoot, file)} changed alpha-channel presence.`);
  }
}

async function createOutput(source, output, dimensions) {
  await mkdir(path.dirname(output.destination), { recursive: true });
  let pipeline = sharp(source, { limitInputPixels: false }).resize({
    width: dimensions.width,
    height: dimensions.height,
    fit: "fill",
    kernel: sharp.kernel.lanczos3,
    withoutEnlargement: true,
  });
  pipeline = output.format === "avif" ? pipeline.avif(output.options) : pipeline.webp(output.options);
  await pipeline.toFile(output.destination);
}

for (const asset of assets) {
  const source = resolve(asset.source);
  await access(source);
  const sourceMetadata = await sharp(source).metadata();
  await assertMetadata(source, asset.dimensions, sourceMetadata.hasAlpha);
  const outputDimensions = asset.resizeWidth
    ? { width: asset.resizeWidth, height: asset.resizeWidth }
    : asset.dimensions;

  for (const output of asset.outputs) {
    const destination = resolve(output.destination);
    if (!verifyOnly) {
      await createOutput(source, { ...output, destination }, outputDimensions);
    }
    await access(destination);
    await assertMetadata(destination, outputDimensions, sourceMetadata.hasAlpha);
  }

  console.log(`${asset.source}  sha256:${await digest(source)}`);
}

console.log(verifyOnly ? "Asset verification passed." : "Optimized website assets generated.");
