#!/usr/bin/env bash

echo "Sjekker eessi-pensjon-begrens-innsyn srvPassord"
if test -f /var/run/secrets/nais.io/srveessi-pensjon-journalforing/password;
then
  echo "Setter eessi-pensjon-begrens-innsyn srvPassord"
    export  SYSTEMBRUKER_PASSWORD=$(cat /var/run/secrets/nais.io/srveessi-pensjon-journalforing/password)
fi

echo "Sjekker eessi-pensjon-begrens-innsyn srvUsername"
if test -f /var/run/secrets/nais.io/srveessi-pensjon-journalforing/username;
then
    echo "Setter eessi-pensjon-begrens-innsyn srvUsername"
    export  SYSTEMBRUKER_USERNAME=$(cat /var/run/secrets/nais.io/srveessi-pensjon-journalforing/username)
fi