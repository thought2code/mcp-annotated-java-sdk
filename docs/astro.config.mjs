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
      lastUpdated: true,
      customCss: ['./src/styles/custom.css'],
      sidebar: [
        {
          label: 'Start Here',
          items: [
            { label: 'Overview', slug: '' },
            { label: 'Getting Started', slug: 'guides/getting-started' },
          ],
        },
        {
          label: 'Reference',
          items: [
            { label: 'Core Components', slug: 'reference/components' },
          ],
        },
      ],
    }),
  ],
});
