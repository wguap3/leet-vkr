import js from '@eslint/js'
import tseslint from 'typescript-eslint'
export default tseslint.config(
  { ignores: ['dist/**', 'node_modules/**', 'playwright-report/**', 'test-results/**'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  { files: ['**/*.{ts,tsx}'], languageOptions: { globals: { window:'readonly', document:'readonly', localStorage:'readonly', self:'readonly', navigator:'readonly', console:'readonly', setTimeout:'readonly', clearTimeout:'readonly', setInterval:'readonly', clearInterval:'readonly', process:'readonly', React:'readonly' } },
    rules: { '@typescript-eslint/no-explicit-any':'off', '@typescript-eslint/no-unused-vars':'off', 'no-unused-vars':'off', 'no-empty':['error', { allowEmptyCatch:true }] } },
  { files: ['eslint.config.js'], languageOptions: { globals: { process:'readonly' } } }
)
