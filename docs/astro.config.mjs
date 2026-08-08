import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://thought2code.github.io',
  base: '/mcp-annotated-java-sdk',
  publicDir: './static',
  integrations: [
    starlight({
      title: 'MCP Annotated Java SDK',
      description: 'Build lightweight MCP servers in plain Java with annotations.',
      logo: {
        src: './static/favicon.svg',
        replacesTitle: false,
      },
      social: [
        {
          icon: 'github',
          label: 'GitHub',
          href: 'https://github.com/thought2code/mcp-annotated-java-sdk',
        },
      ],
      editLink: {
        baseUrl: 'https://github.com/thought2code/mcp-annotated-java-sdk/edit/main/docs/',
      },
      locales: {
        root: { label: 'English', lang: 'en' },
        'zh-cn': { label: '简体中文', lang: 'zh-CN' },
      },
      defaultLocale: 'root',
      lastUpdated: true,
      customCss: ['./src/styles/custom.css'],
      sidebar: [
        {
          label: 'Start Here',
          translations: { 'zh-CN': '从这里开始' },
          items: [
            { label: 'Overview', translations: { 'zh-CN': '概览' }, slug: '' },
            {
              label: 'Getting Started',
              translations: { 'zh-CN': '快速开始' },
              slug: 'guides/getting-started',
            },
          ],
        },
        {
          label: 'Reference',
          translations: { 'zh-CN': '参考' },
          items: [
            {
              label: 'Core Components',
              translations: { 'zh-CN': '核心组件' },
              slug: 'reference/components',
            },
          ],
        },
      ],
    }),
  ],
});
