openssl rand -base64 33
getent hosts cdc-dev1-a.51080.cn.app.example.com   # want 10.27.245.146 (VM1)
getent hosts cdc-dev1-b.51080.cn.app.example.com   # want .147 (VM2)
getent hosts cdc-dev1-c.51080.cn.app.example.com   # want .148 (VM3)
