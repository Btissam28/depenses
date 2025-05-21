package com.example.budget;

import java.util.Date;


class Transaction {
    private Date date;
    private String description;
    private String categorie;
    private double montant; // positif pour revenu, négatif pour dépense

    public Transaction(){}

    public Transaction(Date date, String description, String categorie, double montant) {
        this.date = date;
        this.description = description != null ? description : "";
        this.categorie = categorie != null ? categorie : "";
        this.montant = montant;
    }

    public Date getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getCategorie() {
        return categorie;
    }

    public double getMontant() {
        return montant;
    }
}