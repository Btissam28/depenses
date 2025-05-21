package com.example.budget;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Income {
    private double montant;
    private Date date;
    private String description;

    // Constructeur par défaut requis pour Firebase
    public Income() {
        // Constructeur vide requis pour Firebase
    }

    // Constructeur
    public Income(double montant, Date date, String description) {
        this.montant = montant;
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