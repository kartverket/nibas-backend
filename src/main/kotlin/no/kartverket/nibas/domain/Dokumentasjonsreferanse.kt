package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.LocalDate
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Table(name = "dokumentasjonsreferanse")
@Audited
@Entity
class Dokumentasjonsreferanse {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()
    
    @Column(name = "objtype")
    var objtype: String? = null

    
    @Column(name = "rettskildetittel", nullable = false)
    var rettskildetittel: String? = null

    
    @Column(name = "rettskildeid")
    var rettskildeid: String? = null

    
    @Column(name = "hjemmel")
    var hjemmel: String? = null

    
    @Column(name = "fastsettingsmyndighet")
    var fastsettingsmyndighet: String? = null

    @Column(name = "fastsettingsdato", nullable = false)
    var fastsettingsdato: LocalDate? = null

    @ManyToOne
    @JoinColumn(name = "fylkesgrense_fk")
    var fylkesgrenseFk: Fylkesgrense? = null

    @ManyToOne
    @JoinColumn(name = "kommunegrense_fk")
    var kommunegrenseFk: Kommunegrense? = null

    @ManyToOne
    @JoinColumn(name = "riksgrense_fk")
    var riksgrenseFk: Riksgrense? = null

    @ManyToOne
    @JoinColumn(name = "lovvirkeomraade_fk")
    var lovvirkeomraadeFk: Lovvirkeomraade? = null

    @ManyToOne
    @JoinColumn(name = "lovvirkeomraadegrense_fk")
    var lovvirkeomraadegrenseFk: Lovvirkeomraadegrense? = null

    @ManyToOne
    @JoinColumn(name = "avtaltavgrensningslinje_fk")
    var avtaltavgrensningslinjeFk: Avtaltavgrensningslinje? = null

    @ManyToOne
    @JoinColumn(name = "detsaerskilteomraadet_fk")
    var detsaerskilteomraadetFk: Detsaerskilteomraadet? = null

    @ManyToOne
    @JoinColumn(name = "fiskerisone_fk")
    var fiskerisoneFk: Fiskerisone? = null

    @ManyToOne
    @JoinColumn(name = "fiskevernsone_fk")
    var fiskevernsoneFk: Fiskevernsone? = null

    @ManyToOne
    @JoinColumn(name = "grense200nautiskemil_fk")
    var grense200nautiskemilFk: Grense200nautiskemil? = null

    @ManyToOne
    @JoinColumn(name = "grunnlinje_fk")
    var grunnlinjeFk: Grunnlinje? = null

    @ManyToOne
    @JoinColumn(name = "graasonegrense_fk")
    var graasonegrenseFk: Graasonegrense? = null

    @ManyToOne
    @JoinColumn(name = "graasonen_fk")
    var graasonenFk: Graasonen? = null

    @ManyToOne
    @JoinColumn(name = "indrefarvann_fk")
    var indrefarvannFk: Indrefarvann? = null

    @ManyToOne
    @JoinColumn(name = "kontinentalsokkel_fk")
    var kontinentalsokkelFk: Kontinentalsokkel? = null

    @ManyToOne
    @JoinColumn(name = "norgesoekonomiskesone_fk")
    var norgesoekonomiskesoneFk: Norgesoekonomiskesone? = null

    @ManyToOne
    @JoinColumn(name = "sjoeterritorium_fk")
    var sjoeterritoriumFk: Sjoeterritorium? = null

    @ManyToOne
    @JoinColumn(name = "territorialfarvann_fk")
    var territorialfarvannFk: Territorialfarvann? = null

    @ManyToOne
    @JoinColumn(name = "territorialgrense_fk")
    var territorialgrenseFk: Territorialgrense? = null

    @ManyToOne
    @JoinColumn(name = "tilstoetendesone_fk")
    var tilstoetendesoneFk: Tilstoetendesone? = null

    @ManyToOne
    @JoinColumn(name = "yttergrensesokkel_fk")
    var yttergrensesokkelFk: Yttergrensesokkel? = null

    @ManyToOne
    @JoinColumn(name = "yttergrensetilstoetendesone_fk")
    var yttergrensetilstoetendesoneFk: Yttergrensetilstoetendesone? = null

    @OneToOne(mappedBy = "dokumentasjonsreferanseFk")
    var dokumentlink: DokumentasjonsreferanseDokumentlink? = null
}
