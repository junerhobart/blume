function parseFrontmatter(text) {
  const match = text.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n([\s\S]*)$/);
  if (!match) {
    return { data: {}, body: text };
  }
  const data = {};
  for (const line of match[1].split("\n")) {
    const idx = line.indexOf(":");
    if (idx === -1) continue;
    const key = line.slice(0, idx).trim();
    let value = line.slice(idx + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    data[key] = value;
  }
  return { data, body: match[2] };
}

function iconPath(icon) {
  if (!icon) return "assets/minecraft/textures/item/barrier.png";
  const resourceKey = icon.split("{")[0].split("#")[0];
  const [namespace, key] = resourceKey.split(":", 2);
  if (namespace === "minecraft") return `assets/minecraft/textures/item/${key}.png`;
  if (namespace === "blume") return `assets/textures/${key}.png`;
  return "assets/minecraft/textures/item/barrier.png";
}

async function hasVideo(slug) {
  try {
    const res = await fetch(`assets/videos/${slug}.mp4`, { method: "HEAD" });
    return res.ok;
  } catch {
    return false;
  }
}

function featureHtml(slug, data, html, video) {
  const splitClass = video ? " feature-split" : "";
  const videoHtml = video
    ? `<figure class="feature-video"><video class="feature-video-player w-full mc-border" controls playsinline preload="metadata"><source src="assets/videos/${slug}.mp4" type="video/mp4"></video></figure>`
    : "";
  return `<article class="feature-item">
  <h2 id="${slug}" class="m-0">
    <button type="button" class="feature-toggle mc-interactive relative flex justify-between items-center w-full text-left text-xl font-minecraft font-normal mc-text bg-block-grass mc-border mc-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-transparent px-4 py-3.5" aria-expanded="false" aria-controls="body-${slug}">
      <span class="relative z-10 flex items-center gap-3 min-w-0 flex-1">
        <span class="feature-icon relative w-8 h-8 min-w-[2rem] min-h-[2rem] block">
          <img class="absolute inset-0 object-cover is-pixelated w-full h-full" src="${iconPath(data.icon)}" alt="">
        </span>
        <span class="truncate">${data.title || slug}</span>
      </span>
      <svg data-accordion-icon class="mc-chevron relative z-10 w-5 h-5 shrink-0 text-white" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd"></path></svg>
    </button>
  </h2>
  <div id="body-${slug}" class="mc-accordion-panel">
    <div class="bg-block-grass mc-border border-t-0">
      <div class="feature-body${splitClass}">
        <div class="feature-prose prose prose-invert max-w-none font-minecraft">${html}</div>
        ${videoHtml}
      </div>
    </div>
  </div>
</article>`;
}

async function loadFeature(categoryId, slug) {
  const res = await fetch(`features/${categoryId}/${slug}.md`);
  if (!res.ok) throw new Error(`missing ${categoryId}/${slug}.md`);
  const { data, body } = parseFrontmatter(await res.text());
  const html = marked.parse(body);
  const video = await hasVideo(slug);
  return featureHtml(slug, data, html, video);
}

async function loadSite() {
  const categories = await (await fetch("features/categories.json")).json();
  const root = document.getElementById("features");
  const parts = [];

  for (const category of categories) {
    const features = await Promise.all(
      category.features.map((slug) => loadFeature(category.id, slug))
    );
    parts.push(`<div class="category-block mt-16 mb-6" id="category-${category.id}">
  <h2 class="category-heading font-minecraft text-2xl md:text-3xl mc-text-heading px-5 py-3.5 bg-block-${category.banner} mc-border"><span class="mc-layer-content">${category.title}</span></h2>
</div>
<section class="feature-list">${features.join("")}</section>`);
  }

  root.innerHTML = parts.join("");
  bindMcInteraction();
}

document.addEventListener("DOMContentLoaded", loadSite);
