import nextCoreWebVitals from "eslint-config-next/core-web-vitals";

const eslintConfig = [
  ...nextCoreWebVitals,
  {
    ignores: [
      "eslint.config.mjs",
    ],
  },
];

export default eslintConfig;
