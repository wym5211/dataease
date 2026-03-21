module.exports = {
  env: {
    browser: true,
    es2021: true,
    node: true
  },
  extends: [
    'prettier',
    'plugin:vue/vue3-essential',
    'plugin:@typescript-eslint/recommended',
    'plugin:prettier/recommended' // 解决ESlint和Prettier冲突
  ],
  overrides: [
    {
      files: [
        'src/config/axios/index.ts',
        'src/components/data-visualization/canvas/CanvasCore.vue'
      ],
      rules: {
        '@typescript-eslint/no-explicit-any': 'off'
      }
    }
  ],
  // 配置解析vue文件
  parser: 'vue-eslint-parser',
  parserOptions: {
    ecmaVersion: 'latest',
    parser: '@typescript-eslint/parser',
    sourceType: 'module',
    jsxPragma: 'React',
    ecmaFeatures: {
      jsx: true
    }
  },
  plugins: ['vue', '@typescript-eslint'],
  rules: {
    '@typescript-eslint/ban-types': [
      'error',
      {
        extendDefaults: true,
        types: {
          '{}': false
        }
      }
    ],
    'vue/multi-word-component-names': 0,
    // 将 any 类型从 off 改为 warn，逐步减少 any 的使用
    '@typescript-eslint/no-explicit-any': ['warn'],
    'vue/no-setup-props-destructure': ['off'],
    // 添加更多代码质量规则
    'no-console': ['warn', { allow: ['warn', 'error'] }], // 警告 console.log
    'no-debugger': 'warn', // 警告 debugger
    '@typescript-eslint/no-unused-vars': ['warn', {
      argsIgnorePattern: '^_',
      varsIgnorePattern: '^_'
    }], // 警告未使用的变量
    'prefer-const': 'warn' // 建议使用 const
  }
}
