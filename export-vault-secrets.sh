#!/usr/bin/env bash

echo "Sjekker eessi-pensjon-begrens-innsyn srvPassord"
if test -f /var/run/secrets/nais.io/srveessi-pensjon-journalforing/srvpassword;
then
  echo "Setter eessi-pensjon-begrens-innsyn srvPassord"
    export password=$(cat /var/run/secrets/nais.io/srveessi-pensjon-journalforing/srvpassword)
fi

echo "Sjekker eessi-pensjon-begrens-innsyn srvUsername"
if test -f /var/run/secrets/nais.io/srveessi-pensjon-journalforing/srvusername;
then
    echo "Setter eessi-pensjon-begrens-innsyn srvUsername"
    export username=$(cat /var/run/secrets/nais.io/srveessi-pensjon-journalforing/srvusername)
fi