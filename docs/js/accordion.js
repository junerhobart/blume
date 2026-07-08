var trapdoorOpenSound = "assets/minecraft/sounds/block/wooden_trapdoor/open1.ogg";
var trapdoorCloseSound = "assets/minecraft/sounds/block/wooden_trapdoor/close1.ogg";

function prefersReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function playTrapdoorSound(opening) {
  if (prefersReducedMotion()) return;
  var sound = new Audio(opening ? trapdoorOpenSound : trapdoorCloseSound);
  sound.play().catch(function () {});
}

function setOpenVisuals(el, state) {
  el.classList.toggle("is-open", state);
  var iconEl = el.querySelector("[data-accordion-icon]") || el.querySelector(".mc-chevron");
  if (iconEl) iconEl.classList.toggle("is-open", state);
}

function accordionSetState(el, state, playSound) {
  el.setAttribute("aria-expanded", state ? "true" : "false");
  setOpenVisuals(el, state);
  var panelId = el.getAttribute("aria-controls");
  if (panelId) document.getElementById(panelId).classList.toggle("is-open", state);
  if (playSound) playTrapdoorSound(state);
}

function expandAllInstant(toggles, state) {
  Array.prototype.forEach.call(toggles, function (el) {
    accordionSetState(el, state, false);
  });
  playTrapdoorSound(state);
}

function bindMcInteraction() {
  document.querySelectorAll(".feature-toggle.mc-interactive[aria-controls]").forEach(function (el) {
    el.addEventListener("click", function () {
      accordionSetState(el, el.getAttribute("aria-expanded") !== "true", true);
    });
  });

  var expandCollapseBtn = document.getElementById("expand-collapse-all");
  var allExpanded = false;
  expandCollapseBtn.addEventListener("click", function () {
    allExpanded = !allExpanded;
    expandAllInstant(document.getElementsByClassName("feature-toggle"), allExpanded);
    document.getElementById("expand-collapse-label").textContent = allExpanded ? "Collapse All" : "Expand All";
    accordionSetState(expandCollapseBtn, allExpanded, false);
  });
}
