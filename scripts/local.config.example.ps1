# Optional settings. Copy this file to .runtime/config.ps1 and edit the copy.
# Prefer environment variables for secrets. The .runtime directory is ignored by Git.
# Leave all DB_* settings unset to use the Windows-encrypted private MySQL instance.
#
# $env:SERVER_PORT = '4040'
# $env:FRONTEND_PORT = '5173'
# $env:LINGUAMATE_MYSQL_BIN = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
#
# For your own MySQL server, supply DB_URL and DB_USER together.
# $env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/linguamate?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8'
# $env:DB_USER = 'your_database_user'
# $env:DB_PASSWORD must be supplied in your current environment or securely loaded.
