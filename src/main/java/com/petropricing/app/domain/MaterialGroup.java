package com.petropricing.app.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "material_group")
public class MaterialGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hname")
    private String hname;

    @Column(name = "code_nsi")
    private String codeNsi;

    @Column(name = "v_mtr_name")
    private String vMtrName;

    @Column(name = "mtr_pf_name_lvl_2")
    private String mtrPfNameLvl2;

    @Column(name = "mtr_pf_name_lvl_3")
    private String mtrPfNameLvl3;

    @Column(name = "tlevel")
    private String tlevel;

    @Column(name = "datuv")
    private LocalDate datuv;

    @Column(name = "datub")
    private LocalDate datub;

    @Column(name = "leaf")
    private String leaf;

    public Long getId() {
        return id;
    }

    public String getHname() {
        return hname;
    }

    public void setHname(String hname) {
        this.hname = hname;
    }

    public String getCodeNsi() {
        return codeNsi;
    }

    public void setCodeNsi(String codeNsi) {
        this.codeNsi = codeNsi;
    }

    public String getVMtrName() {
        return vMtrName;
    }

    public void setV_MtrName(String vMtrName) {
        this.vMtrName = vMtrName;
    }

    public String getMtrPfNameLvl2() {
        return mtrPfNameLvl2;
    }

    public void setMtrPfNameLvl2(String mtrPfNameLvl2) {
        this.mtrPfNameLvl2 = mtrPfNameLvl2;
    }

    public String getMtrPfNameLvl3() {
        return mtrPfNameLvl3;
    }

    public void setMtrPfNameLvl3(String mtrPfNameLvl3) {
        this.mtrPfNameLvl3 = mtrPfNameLvl3;
    }

    public String getTlevel() {
        return tlevel;
    }

    public void setTlevel(String tlevel) {
        this.tlevel = tlevel;
    }

    public LocalDate getDatuv() {
        return datuv;
    }

    public void setDatuv(LocalDate datuv) {
        this.datuv = datuv;
    }

    public LocalDate getDatub() {
        return datub;
    }

    public void setDatub(LocalDate datub) {
        this.datub = datub;
    }

    public String getLeaf() {
        return leaf;
    }

    public void setLeaf(String leaf) {
        this.leaf = leaf;
    }
}

