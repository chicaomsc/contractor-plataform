# Upload Policy

**Sprint:** 9 — MVP Hardening  
**Data:** 2026-07-16

## Formatos Permitidos

- PNG (`image/png`, `.png`)
- JPEG (`image/jpeg`, `.jpg`, `.jpeg`)
- WebP (`image/webp`, `.webp`)

## Limites

- Tamanho máximo: 5 MB.
- Arquivo vazio é rejeitado.
- MIME type deve ser permitido.
- Extensão deve corresponder ao MIME type.
- Assinatura binária deve corresponder ao formato declarado.

## Bloqueios

- SVG não é permitido nesta fase.
- Executáveis, texto, HTML e arquivos com MIME disfarçado são rejeitados.
- Path traversal em storage local é rejeitado.
- Exclusão aceita apenas paths públicos iniciados por `/uploads/` e normalizados dentro da pasta base.

## Cache Público

`/uploads/**` usa cache público de 30 dias no resource handler local.

## Implementação

- `ImageUploadPolicy`
- `GalleryImageService`
- `CompanyService`
- `LocalStorageService`
- `StorageWebConfig`
- `UploadArea` no frontend aceita somente PNG/JPEG/WebP.
