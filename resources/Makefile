all: client.p12 server.p12 demoCA/private/ca.p12

#HOSTNAME=here.mtls.proxy.com
HOSTNAME=localhost

%key.pem:
	openssl genrsa -out $@ 4096

demoCA/cacert.pem: demoCA/private/cakey.pem
	openssl req -new -x509 -days 3650 -key $^ -out $@ -subj "/C=GB/ST=London/L=London/O=Global Security/OU=IT Department/CN=ca-server"

demoCA/private/ca.p12: demoCA/cacert.pem demoCA/private/cakey.pem
	openssl pkcs12 -export -out $@ -in demoCA/cacert.pem -inkey demoCA/private/cakey.pem -passout pass:changeme

server.csr: server.key.pem
	openssl req -new -key $^ -out $@ -subj "/C=GB/ST=London/L=London/O=Global Security/OU=IT Department/CN=$(HOSTNAME)" 

server.p12: server.key.pem server.crt.pem
	openssl pkcs12 -export -out $@ -in server.crt.pem -inkey server.key.pem -passout pass:changeme

client.csr: client.key.pem
	openssl req -new -key $^ -out $@ -subj "/C=GB/ST=London/L=London/O=Global Security/OU=IT Department/CN=client" -addext 'extendedKeyUsage=clientAuth'

client.p12: client.key.pem client.crt.pem
	openssl pkcs12 -export -out $@ -in client.crt.pem -inkey client.key.pem -passout pass:changeme

%.crt.pem: %.csr demoCA/private/cakey.pem demoCA/cacert.pem demoCA/serial demoCA/index.txt
	openssl ca -days 1650 -notext -batch -in $< -out $@

demoCA/serial:
	echo 01 > demoCA/serial

demoCA/index.txt:
	touch demoCA/index.txt

.PHONY: clean
clean:
	@rm -r demoCA *.pem *.crt *.csr || true
	mkdir -p demoCA/private
	mkdir -p demoCA/newcerts
