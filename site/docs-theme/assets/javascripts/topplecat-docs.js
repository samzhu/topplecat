(() => {
  const fallbackCopy = (value) => {
    const textarea = document.createElement("textarea");
    textarea.value = value;
    textarea.setAttribute("readonly", "");
    textarea.style.position = "fixed";
    textarea.style.opacity = "0";
    document.body.appendChild(textarea);
    textarea.select();
    const copied = document.execCommand("copy");
    textarea.remove();
    if (!copied) throw new Error("Clipboard unavailable");
  };

  const copyMarkdown = async (button) => {
    const status = button.closest("[data-doc-toolbar]")?.querySelector(".topplecat-copy-status");
    const label = button.querySelector("[data-copy-label]");
    const original = label?.textContent || "Copy Markdown";
    try {
      const url = new URL(button.dataset.copyMarkdown, document.baseURI);
      const response = await fetch(url, { credentials: "same-origin" });
      if (!response.ok) throw new Error(`Markdown request failed: ${response.status}`);
      const markdown = await response.text();
      if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(markdown);
      else fallbackCopy(markdown);
      label.textContent = button.dataset.copiedLabel || "Copied";
      if (status) status.textContent = button.dataset.copiedLabel || "Copied";
      window.setTimeout(() => {
        label.textContent = original;
        if (status) status.textContent = "";
      }, 1800);
    } catch {
      if (status) status.textContent = button.dataset.copyError || "Copy unavailable";
    }
  };

  const bind = () => document.querySelectorAll("[data-copy-markdown]").forEach((button) => {
    if (button.dataset.copyBound) return;
    button.dataset.copyBound = "true";
    button.addEventListener("click", () => copyMarkdown(button));
  });
  bind();
  document.addEventListener("DOMContentLoaded", bind);
})();
