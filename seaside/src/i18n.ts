import type { I18n, I18nOptions, Locale } from 'vue-i18n';
import { createI18n } from 'vue-i18n';
import { isRef, nextTick } from 'vue';
import { NavigationGuardWithThis } from 'vue-router';

const i18n = createI18n({
    legacy: false,
    locale: navigator.language,
    fallbackLocale: 'en',
});

export default i18n;

export const SUPPORT_LOCALES = ['en', 'fr'];

function isComposer(i18n: I18n): boolean {
    return i18n.mode === 'composition' && isRef(i18n.global.locale);
}

export function getLocale(i18n: I18n): string {
    if (isComposer(i18n)) {
        return i18n.global.locale.value;
    } else {
        return i18n.global.locale;
    }
}

function getFallbackLocale(i18n: I18n): string {
    if (isComposer(i18n)) {
        return i18n.global.fallbackLocale.value;
    } else {
        return i18n.global.fallbackLocale;
    }
}

export function setLocale(i18n: I18n, locale: Locale): void {
    if (isComposer(i18n)) {
        i18n.global.locale.value = locale;
    } else {
        i18n.global.locale = locale;
    }
}

export function setupI18n(options: I18nOptions = { locale: 'en' }): I18n {
    const i18n = createI18n(options);
    setI18nLanguage(i18n, options.locale!);
    return i18n;
}

function setI18nLanguage(i18n: I18n, locale: Locale): void {
    setLocale(i18n, locale);
    /**
     * NOTE:
     * If you need to specify the language setting for headers, such as the `fetch` API, set it here.
     * The following is an example for axios.
     *
     * axios.defaults.headers.common['Accept-Language'] = locale
     */
    document.querySelector('html')!.setAttribute('lang', locale);
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const getResourceMessages = (r: any) => r.default || r;

async function loadLocaleMessages(i18n: I18n, locale: Locale) {
    // load locale messages
    const messages = await import(`./locales/${locale}.json`).then(
        getResourceMessages,
    );

    // set locale and locale message
    i18n.global.setLocaleMessage(locale, messages);

    return nextTick();
}

export const setupI18nLocal: NavigationGuardWithThis<NavigationGuardWithThis<boolean>> = async to => {
    const locale = getLocale(i18n);
    console.log(`locale: ${locale}`);

    // use locale if paramsLocale is not in SUPPORT_LOCALES
    const displayedLocale = (SUPPORT_LOCALES.includes(locale)) ? locale : getFallbackLocale(i18n);

    // load locale messages
    if (!i18n.global.availableLocales.includes(displayedLocale)) {
        await loadLocaleMessages(i18n, displayedLocale);
    }

    // set i18n language
    setI18nLanguage(i18n, displayedLocale);
};
