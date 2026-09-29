/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Raiz da API, sem barra final. Ex.: http://localhost:8080 */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
