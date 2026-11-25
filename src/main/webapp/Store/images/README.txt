PrintKraft product images
==========================

Product-detail pages use three image positions per product.

Folder convention
-----------------
src/main/webapp/Store/images/product-001/image-1.webp
src/main/webapp/Store/images/product-001/image-2.webp
src/main/webapp/Store/images/product-001/image-3.webp

Replace 001 with the database products.product_id padded to at least three digits.
Use WebP for automatically discovered images. The application falls back to these paths
when a product has missing image_url rows or fewer than three configured images.

Customize a product
-------------------
You can change a product's path without changing Java code by editing product_images.image_url.
Use display_order 1, 2, 3 and mark display_order=1 as is_primary=1. The product detail
query filters empty/null image URLs, and the large image is synchronized with the first
thumbnail.

The provided sample package contains the actual images for product folders 001, 002 and 003.
The SQL seed maps the first three demo products to these images and adds products 011–060.
Because this repository already seeds ten demo catalog items, the new SQL inserts the next
50 catalog entries. It uses AUTO_INCREMENT to avoid overwriting existing product IDs; in a
fresh demo database these IDs are normally 11–60.
