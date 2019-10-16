package no.nav.eessi.pensjon.services.personv3


enum class Diskresjonskode(term: String) {
    KLIE("Klientadresse"),
    MILI("Militær"),
    PEND("Pendler"),
    SPFO("Sperret adresse, fortrolig"),
    SPSF("Sperret adresse, strengt fortrolig"),
    SVAL("Svalbard"),
    UFB("Uten fast bopel"),
    URIK("I utenrikstjeneste")
}