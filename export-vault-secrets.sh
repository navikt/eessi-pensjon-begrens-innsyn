#!/usr/bin/env bash

if test -f /var/run/secrets/nais.io/srveessi-pensjon-j/password;
then
    export  SYSTEMBRUKER_PASSWORD=$(cat /var/run/secrets/nais.io/srveessi-pensjon-j/password)
    echo "Setting SYSTEMBRUKER_PASSWORD"
fi

if test -f /var/run/secrets/nais.io/srveessi-pensjon-j/username;
then
    export  SYSTEMBRUKER_USERNAME=$(cat /var/run/secrets/nais.io/srveessi-pensjon-j/username)
    echo "Setting SYSTEMBRUKER_USERNAME"
fi