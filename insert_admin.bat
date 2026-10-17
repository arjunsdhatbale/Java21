@echo off
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot demo -e "INSERT INTO users (first_name, last_name, email, password, phone, role, status, auth_provider, created_at, updated_at) VALUES ('Arjun','Dhatbale','arjun@example.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','9876543210','ADMIN','ACTIVE','LOCAL',NOW(),NOW())"
echo Admin insert exit code: %ERRORLEVEL%
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot demo -e "INSERT INTO users (first_name, last_name, email, password, phone, role, status, auth_provider, created_at, updated_at) VALUES ('Demo','User','user@example.com','$2a$10$slYQmyNdgTY18LGqqgMkCuCUSI5Y5KRyBbkJHQjLKPUSSj9kDiDGm','9998887770','USER','ACTIVE','LOCAL',NOW(),NOW())"
echo User insert exit code: %ERRORLEVEL%
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot demo -e "SELECT id, first_name, email, role, status FROM users"
