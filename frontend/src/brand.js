export const BRAND_NAME = "语伴智学";
export const BRAND_ENGLISH_NAME = "LinguaMate";
export const BRAND_FULL_NAME = `${BRAND_NAME}（${BRAND_ENGLISH_NAME}）`;

export function brandedPageTitle(page) {
  return page ? `${page} · ${BRAND_FULL_NAME}` : BRAND_FULL_NAME;
}
