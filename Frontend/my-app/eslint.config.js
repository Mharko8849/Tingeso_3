import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import reactPlugin from 'eslint-plugin-react';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import importPlugin from 'eslint-plugin-import';
import { defineConfig, globalIgnores } from 'eslint/config';

// Airbnb JavaScript Style Guide rules applied via flat config
export default defineConfig([
  globalIgnores(['dist', 'node_modules']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs['recommended-latest'],
      reactRefresh.configs.vite,
    ],
    plugins: {
      react: reactPlugin,
      'jsx-a11y': jsxA11y,
      import: importPlugin,
    },
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
      parserOptions: {
        ecmaVersion: 'latest',
        ecmaFeatures: { jsx: true },
        sourceType: 'module',
      },
    },
    settings: {
      react: {
        version: 'detect',
      },
    },
    rules: {
      // Airbnb Style Guide - Core JS rules
      'no-unused-vars': ['warn', { varsIgnorePattern: '^[A-Z_]', argsIgnorePattern: '^_', caughtErrorsIgnorePattern: '^_' }],
      'no-console': ['warn', { allow: ['warn', 'error', 'debug'] }],
      'prefer-const': 'error',
      'no-var': 'error',
      'eqeqeq': ['error', 'always', { null: 'ignore' }],
      'no-multiple-empty-lines': ['warn', { max: 1 }],
      'comma-dangle': ['warn', 'always-multiline'],
      'semi': ['warn', 'always'],
      'quotes': ['warn', 'single', { avoidEscape: true }],
      'arrow-body-style': ['warn', 'as-needed'],
      'prefer-arrow-callback': 'warn',
      'object-shorthand': ['warn', 'always'],
      'prefer-template': 'warn',
      'no-param-reassign': ['warn', { props: false }],

      // Airbnb Style Guide - React rules
      'react/prop-types': 'warn',
      'react/jsx-uses-react': 'off',
      'react/react-in-jsx-scope': 'off',
      'react/jsx-filename-extension': ['warn', { extensions: ['.jsx', '.js'] }],
      'react/jsx-props-no-spreading': 'off',
      'react/function-component-definition': ['warn', { namedComponents: 'arrow-function' }],
      'react/self-closing-comp': 'warn',

      // Airbnb Style Guide - Import rules
      'import/prefer-default-export': 'off',
      'import/no-extraneous-dependencies': 'off',

      // Airbnb Style Guide - Accessibility rules
      'jsx-a11y/anchor-is-valid': 'warn',
      'jsx-a11y/label-has-associated-control': ['warn', { assert: 'either' }],
      'jsx-a11y/click-events-have-key-events': 'warn',
      'jsx-a11y/no-noninteractive-element-interactions': 'warn',
      'jsx-a11y/interactive-supports-focus': 'warn',

      // Allow multiple exports (context + provider pattern)
      'react-refresh/only-export-components': 'off',
    },
  },
  // Test files configuration
  {
    files: ['**/*.test.{js,jsx}', '**/*.spec.{js,jsx}', '**/setupTests.js'],
    languageOptions: {
      globals: {
        ...globals.jest,
        vi: 'readonly',
        vitest: 'readonly',
      },
    },
    rules: {
      'no-unused-vars': 'off',
      'react/prop-types': 'off',
    },
  },
]);
