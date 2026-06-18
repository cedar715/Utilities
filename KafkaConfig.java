curl -X POST \
  -u "admin:password" \
  -H "Content-Type: application/json" \
  -d '{
    "syslogName": "MyRemoteSyslog",
    "host": "192.168.1.50:514",
    "transport": "tcp",
    "facility": "event"
  }' \
  "http://<broker-ip>:8080/SEMP/v2/config/syslogs"

    curl -X PATCH \
  -u "admin:password" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true
  }' \
  "http://<broker-ip>:8080/SEMP/v2/config/syslogs/MyRemoteSyslog"

    curl -X GET \
  -u "admin:password" \
  "http://<broker-ip>:8080/SEMP/v2/config/syslogs/MyRemoteSyslog"


    POST http://<broker>:8080/SEMP    (or your SEMP port)
Content-Type: application/xml
Auth: admin / <password>

<rpc semp-version="soltr/10_25VMR">
  <rpc>
    <logging>
      <syslog>
        <name>external</name>
        <host>
          <host-address><syslog-server-ip>:<port></host-address>
          <transport><tcp-or-udp></transport>
        </host>
        <facility>
          <event/>
          <command/>
          <system/>
        </facility>
        <no><shutdown/></no>
      </syslog>
    </logging>
  </rpc>
</rpc>
