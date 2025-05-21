package com.example.budget;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Expenses {
    private double montant;
    private String categorie;
    private Date date;
    private String description;

    // Constructeur par défaut requis pour Firebase
    public Expenses() {
        // Constructeur vide requis pour Firebase
    }

    // Constructeur
    public Expenses(double montant, String categorie, Date date, String description) {
        this.montant = montant;
        this.categorie = categorie;
        this.date = date;
        this.description = description;
    }

    // Getters et setters
    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Méthode pour convertir la date en String (utilisée lors de l'enregistrement dans Firebase ou l'affichage)
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(date);
    }
}