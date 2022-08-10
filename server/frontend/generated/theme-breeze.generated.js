import 'construct-style-sheets-polyfill';
import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles';

const createLinkReferences = (css, target) => {
  // Unresolved urls are written as '@import url(text);' to the css
  // [0] is the full match
  // [1] matches the media query
  // [2] matches the url
  const importMatcher = /(?:@media\s(.+?))?(?:\s{)?\@import\surl\((.+?)\);(?:})?/g;
  
  var match;
  var styleCss = css;
  
  // For each external url import add a link reference
  while((match = importMatcher.exec(css)) !== null) {
    styleCss = styleCss.replace(match[0], "");
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = match[2];
    if (match[1]) {
      link.media = match[1];
    }
    // For target document append to head else append to target
    if (target === document) {
      document.head.appendChild(link);
    } else {
      target.appendChild(link);
    }
  };
  return styleCss;
};

// target: Document | ShadowRoot
export const injectGlobalCss = (css, target, first) => {
  if(target === document) {
    const hash = getHash(css);
    if (window.Vaadin.theme.injectedGlobalCss.indexOf(hash) !== -1) {
      return;
    }
    window.Vaadin.theme.injectedGlobalCss.push(hash);
  }
  const sheet = new CSSStyleSheet();
  sheet.replaceSync(createLinkReferences(css,target));
  if (first) {
    target.adoptedStyleSheets = [sheet, ...target.adoptedStyleSheets];
  } else {
    target.adoptedStyleSheets = [...target.adoptedStyleSheets, sheet];
  }
};
import stylesCss from 'themes/breeze/styles.css?inline';
import { color } from '@vaadin/vaadin-lumo-styles/color.js';
import { typography } from '@vaadin/vaadin-lumo-styles/typography.js';
import commonFieldLabelCss from 'themes/breeze/components/common-field-label.css?inline';
import commonInputFieldCss from 'themes/breeze/components/common-input-field.css?inline';
import commonItemStylesCss from 'themes/breeze/components/common-item-styles.css?inline';
import vaadinAccordionPanelCss from 'themes/breeze/components/vaadin-accordion-panel.css?inline';
import vaadinButtonCss from 'themes/breeze/components/vaadin-button.css?inline';
import vaadinCheckboxGroupCss from 'themes/breeze/components/vaadin-checkbox-group.css?inline';
import vaadinCheckboxCss from 'themes/breeze/components/vaadin-checkbox.css?inline';
import vaadinComboBoxItemCss from 'themes/breeze/components/vaadin-combo-box-item.css?inline';
import vaadinComboBoxOverlayCss from 'themes/breeze/components/vaadin-combo-box-overlay.css?inline';
import vaadinComboBoxCss from 'themes/breeze/components/vaadin-combo-box.css?inline';
import vaadinContextMenuListBoxCss from 'themes/breeze/components/vaadin-context-menu-list-box.css?inline';
import vaadinContextMenuOverlayCss from 'themes/breeze/components/vaadin-context-menu-overlay.css?inline';
import vaadinDatePickerCss from 'themes/breeze/components/vaadin-date-picker.css?inline';
import vaadinDateTimePickerCustomFieldCss from 'themes/breeze/components/vaadin-date-time-picker-custom-field.css?inline';
import vaadinGridCss from 'themes/breeze/components/vaadin-grid.css?inline';
import vaadinInputContainerCss from 'themes/breeze/components/vaadin-input-container.css?inline';
import vaadinItemCss from 'themes/breeze/components/vaadin-item.css?inline';
import vaadinMenuBarButtonCss from 'themes/breeze/components/vaadin-menu-bar-button.css?inline';
import vaadinNumberFieldCss from 'themes/breeze/components/vaadin-number-field.css?inline';
import vaadinRadioButtonCss from 'themes/breeze/components/vaadin-radio-button.css?inline';
import vaadinRadioGroupCss from 'themes/breeze/components/vaadin-radio-group.css?inline';
import vaadinSelectOverlayCss from 'themes/breeze/components/vaadin-select-overlay.css?inline';
import vaadinSelectCss from 'themes/breeze/components/vaadin-select.css?inline';
import vaadinTabCss from 'themes/breeze/components/vaadin-tab.css?inline';
import vaadinTextAreaCss from 'themes/breeze/components/vaadin-text-area.css?inline';
import vaadinTextFieldCss from 'themes/breeze/components/vaadin-text-field.css?inline';
import vaadinTimePickerCss from 'themes/breeze/components/vaadin-time-picker.css?inline';

window.Vaadin = window.Vaadin || {};
window.Vaadin.theme = window.Vaadin.theme || {};
window.Vaadin.theme.injectedGlobalCss = [];

/**
 * Calculate a 32 bit FNV-1a hash
 * Found here: https://gist.github.com/vaiorabbit/5657561
 * Ref.: http://isthe.com/chongo/tech/comp/fnv/
 *
 * @param {string} str the input value
 * @returns {string} 32 bit (as 8 byte hex string)
 */
function hashFnv32a(str) {
  /*jshint bitwise:false */
  let i, l, hval = 0x811c9dc5;

  for (i = 0, l = str.length; i < l; i++) {
    hval ^= str.charCodeAt(i);
    hval += (hval << 1) + (hval << 4) + (hval << 7) + (hval << 8) + (hval << 24);
  }

  // Convert to 8 digit hex string
  return ("0000000" + (hval >>> 0).toString(16)).substr(-8);
}

/**
 * Calculate a 64 bit hash for the given input.
 * Double hash is used to significantly lower the collision probability.
 *
 * @param {string} input value to get hash for
 * @returns {string} 64 bit (as 16 byte hex string)
 */
function getHash(input) {
  let h1 = hashFnv32a(input); // returns 32 bit (as 8 byte hex string)
  return h1 + hashFnv32a(h1 + input); 
}
export const applyTheme = (target) => {
  
  injectGlobalCss(stylesCss.toString(), target);
    
  
  if (!document['_vaadintheme_breeze_componentCss']) {
    registerStyles(
      'common-field-label',
      unsafeCSS(commonFieldLabelCss.toString())
    );
    registerStyles(
      'common-input-field',
      unsafeCSS(commonInputFieldCss.toString())
    );
    registerStyles(
      'common-item-styles',
      unsafeCSS(commonItemStylesCss.toString())
    );
    registerStyles(
      'vaadin-accordion-panel',
      unsafeCSS(vaadinAccordionPanelCss.toString())
    );
    registerStyles(
      'vaadin-button',
      unsafeCSS(vaadinButtonCss.toString())
    );
    registerStyles(
      'vaadin-checkbox-group',
      unsafeCSS(vaadinCheckboxGroupCss.toString())
    );
    registerStyles(
      'vaadin-checkbox',
      unsafeCSS(vaadinCheckboxCss.toString())
    );
    registerStyles(
      'vaadin-combo-box-item',
      unsafeCSS(vaadinComboBoxItemCss.toString())
    );
    registerStyles(
      'vaadin-combo-box-overlay',
      unsafeCSS(vaadinComboBoxOverlayCss.toString())
    );
    registerStyles(
      'vaadin-combo-box',
      unsafeCSS(vaadinComboBoxCss.toString())
    );
    registerStyles(
      'vaadin-context-menu-list-box',
      unsafeCSS(vaadinContextMenuListBoxCss.toString())
    );
    registerStyles(
      'vaadin-context-menu-overlay',
      unsafeCSS(vaadinContextMenuOverlayCss.toString())
    );
    registerStyles(
      'vaadin-date-picker',
      unsafeCSS(vaadinDatePickerCss.toString())
    );
    registerStyles(
      'vaadin-date-time-picker-custom-field',
      unsafeCSS(vaadinDateTimePickerCustomFieldCss.toString())
    );
    registerStyles(
      'vaadin-grid',
      unsafeCSS(vaadinGridCss.toString())
    );
    registerStyles(
      'vaadin-input-container',
      unsafeCSS(vaadinInputContainerCss.toString())
    );
    registerStyles(
      'vaadin-item',
      unsafeCSS(vaadinItemCss.toString())
    );
    registerStyles(
      'vaadin-menu-bar-button',
      unsafeCSS(vaadinMenuBarButtonCss.toString())
    );
    registerStyles(
      'vaadin-number-field',
      unsafeCSS(vaadinNumberFieldCss.toString())
    );
    registerStyles(
      'vaadin-radio-button',
      unsafeCSS(vaadinRadioButtonCss.toString())
    );
    registerStyles(
      'vaadin-radio-group',
      unsafeCSS(vaadinRadioGroupCss.toString())
    );
    registerStyles(
      'vaadin-select-overlay',
      unsafeCSS(vaadinSelectOverlayCss.toString())
    );
    registerStyles(
      'vaadin-select',
      unsafeCSS(vaadinSelectCss.toString())
    );
    registerStyles(
      'vaadin-tab',
      unsafeCSS(vaadinTabCss.toString())
    );
    registerStyles(
      'vaadin-text-area',
      unsafeCSS(vaadinTextAreaCss.toString())
    );
    registerStyles(
      'vaadin-text-field',
      unsafeCSS(vaadinTextFieldCss.toString())
    );
    registerStyles(
      'vaadin-time-picker',
      unsafeCSS(vaadinTimePickerCss.toString())
    );
    
    document['_vaadintheme_breeze_componentCss'] = true;
  }
  injectGlobalCss(color.cssText, target, true);
injectGlobalCss(typography.cssText, target, true);

}
