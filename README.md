Test with mutual authentication:

  while true ; do curl -k -v --cert resources/example.com.p12 --cert-type p12 --pass changeme https://localhost:8443 ; done

(this is cheating: you wouldn't normally use the same certificate for both the client and the server - but this is sufficient for this test project)

To run the integration test, with that specific url or any other you might choose, you will have to update your etc/hosts:
"127.0.0.1 here.mtls.proxy.com"
