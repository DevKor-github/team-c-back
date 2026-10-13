-- S3 image URL migration draft only.
-- Do not execute automatically. Back up the database and verify object parity first.
-- This script changes URLs only when they use the old S3 host.

START TRANSACTION;

UPDATE tb_file
SET file_saved_name = REPLACE(
    file_saved_name,
    'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com',
    'https://kodaero-images.s3.ap-southeast-2.amazonaws.com'
)
WHERE file_saved_name LIKE
      'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com/%';

UPDATE tb_file
SET thumb_saved_name = REPLACE(
    thumb_saved_name,
    'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com',
    'https://kodaero-images.s3.ap-southeast-2.amazonaws.com'
)
WHERE thumb_saved_name LIKE
      'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com/%';

UPDATE tb_building
SET image_url = REPLACE(
    image_url,
    'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com',
    'https://kodaero-images.s3.ap-southeast-2.amazonaws.com'
)
WHERE image_url LIKE
      'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com/%';

UPDATE tb_building_image
SET image = REPLACE(
    image,
    'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com',
    'https://kodaero-images.s3.ap-southeast-2.amazonaws.com'
)
WHERE image LIKE
      'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com/%';

UPDATE tb_place
SET image_url = REPLACE(
    image_url,
    'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com',
    'https://kodaero-images.s3.ap-southeast-2.amazonaws.com'
)
WHERE image_url LIKE
      'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com/%';

UPDATE tb_character
SET image_url = REPLACE(
    image_url,
    'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com',
    'https://kodaero-images.s3.ap-southeast-2.amazonaws.com'
)
WHERE image_url LIKE
      'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com/%';

UPDATE tb_pub_image
SET image_url = REPLACE(
    image_url,
    'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com',
    'https://kodaero-images.s3.ap-southeast-2.amazonaws.com'
)
WHERE image_url LIKE
      'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com/%';

UPDATE tb_suggestion_image
SET image_url = REPLACE(
    image_url,
    'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com',
    'https://kodaero-images.s3.ap-southeast-2.amazonaws.com'
)
WHERE image_url LIKE
      'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com/%';

UPDATE tb_push_dispatch
SET image_url = REPLACE(
    image_url,
    'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com',
    'https://kodaero-images.s3.ap-southeast-2.amazonaws.com'
)
WHERE image_url LIKE
      'https://kodaero-ku.s3.ap-northeast-2.amazonaws.com/%';

-- Review row counts and verify changed URLs before committing.
-- COMMIT;
-- ROLLBACK;
