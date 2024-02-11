import { defineConfig } from 'vite';
import { resolve } from 'path';
import vue from '@vitejs/plugin-vue';
import vueI18n from '@intlify/unplugin-vue-i18n/vite';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    define: {
        'process.env.NODE_ENV': JSON.stringify(mode),
    },
    plugins: [vue(), vueI18n({
        include: resolve(__dirname, './src/locales/**'),
    })],
    server: {
        port: 8080,
        cors: true,
        proxy: {
            '^/api': {
                target: 'http://localhost:8081',
                toProxy: true,
                timeout: 0,
            },
            '^/graphiql': {
                target: 'http://localhost:8081',
                toProxy: true,
                timeout: 0,
            },
            '^/gpi': {
                target: 'http://localhost:8081',
                toProxy: true,
                timeout: 0,
            },
            '^/img': {
                target: 'http://localhost:8082',
                toProxy: true,
                timeout: 0,
                rewrite: (path) => path.replace(/^\/img/, ''),
            },
        },
    },
}));
