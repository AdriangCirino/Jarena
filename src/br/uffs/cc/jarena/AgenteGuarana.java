package br.uffs.cc.jarena;

public class AgenteGuarana extends Agente {

    public AgenteGuarana(Integer x, Integer y, Integer energia) {
        super(x, y, energia);
    }

    @Override
    public void pensa() {
        para();
    }

    @Override
    public void recebeuEnergia() {
    }

    @Override
    public void tomouDano(int energiaRestanteInimigo) {
    }

    @Override
    public void ganhouCombate() {
    }

    @Override
    public void recebeuMensagem(String msg) {
    }

    @Override
    public String getEquipe() {
        return "Guarana";
    }
}