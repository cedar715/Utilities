    healthcheck:
      test: >-
        code=$(/usr/bin/curl -sk -o /dev/null -w "%{http_code}"
        https://127.0.0.1:5553/health-check/guaranteed-active
        2>/dev/null);
        [ "$$code" = "200" ] || [ "$$code" = "503" ]
