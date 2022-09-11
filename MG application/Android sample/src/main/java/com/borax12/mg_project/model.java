package com.borax12.mg_project;

public class model
{
    String remain,purchase,pay;

    public model(String remain, String purchase, String pay)
    {
        this.remain = remain;
        this.purchase = purchase;
        this.pay = pay;
    }

    public String getRemain() { return remain; }

    public void setRemain(String remain) { this.remain = remain; }

    public String getPurchase() { return purchase; }

    public void setPurchase(String purchase) { this.purchase = purchase; }

    public String getPay() { return pay; }

    public void setPay(String pay) { this.pay = pay; }
}

