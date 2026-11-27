# Customer avatar storage

Uploaded customer avatars are written here as normalized JPEG files with random names. These files are runtime user data and are excluded from Git.

The application normally resolves this directory from the deployed web application's `/Store/Avatar` path. When the container serves a packed WAR or when persistent storage is mounted elsewhere, configure `PK_AVATAR_DIR` to the absolute directory used for avatar files. The same setting must be available to both the profile upload controller and the authenticated `/account-avatar` endpoint.

Avatar files should be served through `/account-avatar`, not as public static files.
