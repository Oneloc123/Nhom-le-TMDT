import { sampleSettings } from "../data.js";
import { badge, esc, icon, pageHeading, panel } from "../components.js";

const groups = [
  ["printingTechnologies", "Printing technology"],
  ["materials", "Materials"],
  ["surfaceQualities", "Surface quality"]
];

export function renderSettings(state) {
  const settings = { ...sampleSettings, ...(state.settings || {}) };
  const checked = settings.requireProductReview ? "true" : "false";
  const chips = (key, values = []) => `<div class="tag-row settings-tags">${values.map(value => `<span class="tag">${esc(value)}<button type="button" data-action="remove-setting-tag" data-tag-group="${key}" data-tag="${esc(value)}" aria-label="Remove ${esc(value)}">×</button></span>`).join("")}<button type="button" class="tag add-tag" data-action="add-setting-tag" data-tag-group="${key}">${icon("plus")} Add</button></div>`;
  const general = `<section class="setting-group" id="settings-general"><h3>General</h3><p>Marketplace name and regional preferences.</p><div class="form-grid"><label class="form-field">Platform name<input name="platformName" value="${esc(settings.platformName || "")}" required></label><label class="form-field">Currency<input name="currency" value="${esc(settings.currency || "")}" required></label><label class="form-field wide">Timezone<input name="timezone" value="${esc(settings.timezone || "")}" required></label></div></section>`;
  const market = `<section class="setting-group" id="settings-marketplace"><h3>Marketplace</h3><p>Changes are saved to the MySQL admin settings record.</p><div class="form-grid"><label class="form-field">Default commission (%)<input name="commissionRate" type="number" min="0" max="100" step="0.5" value="${esc(settings.commissionRate ?? state.commissionRate ?? 10)}"></label><label class="form-field">Required seller deposit<input name="requiredDeposit" type="number" min="0" step="500000" value="${esc(settings.requiredDeposit ?? 0)}"></label></div><div class="toggle-row"><span class="toggle-copy"><strong>Require product review</strong><small>Products remain pending until an admin approves them.</small></span><button type="button" class="switch" role="switch" aria-checked="${checked}" data-action="settings-toggle" aria-label="Toggle product review"></button></div><div class="toggle-row"><span class="toggle-copy"><strong>Seller registration</strong><small>Current workspace does not require admin approval for sellers.</small></span>${badge("Enabled", "success")}</div></section>`;
  const printing = `<section class="setting-group" id="settings-printing"><h3>3D printing options</h3><p>Reference options available to seller listings.</p>${groups.map(([key, label]) => `<label class="form-field setting-label">${label}</label>${chips(key, settings[key] || [])}`).join("")}</section>`;
  return `${pageHeading("WORKSPACE CONFIGURATION", "Marketplace settings", "Configuration is loaded from and saved to MySQL.", `<button class="button" form="marketplace-settings" type="submit">${icon("check")} Save settings</button>`)}<div class="settings-nav">${["General", "Marketplace", "3D printing"].map((tab, index) => `<button data-settings-scroll="settings-${["general", "marketplace", "printing"][index]}">${tab}</button>`).join("")}</div>${panel("Platform configuration", "MySQL admin workspace settings", `<form id="marketplace-settings" data-form="settings">${general}${market}${printing}<div class="setting-footer"><span>${icon("check")} Changes are persisted in the admin database</span><button class="button" type="submit">Save changes</button></div></form>`)}`;
}
