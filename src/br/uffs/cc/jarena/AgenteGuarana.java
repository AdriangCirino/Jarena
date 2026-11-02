package br.uffs.cc.jarena;

public class AgenteGuarana extends Agente {

    private int turno;
    private boolean primeiroTurno = true;

    public AgenteGuarana(Integer x, Integer y, Integer energia) {
        super(x, y, energia);
    }

    @Override
    public void pensa() {
        turno++;

        if (primeiroTurno) {
            primeiroTurno = false;
            para();
            return;
        }

        // Andar custa 3 de energia por turno (viver + andar) e ficar parado
        // custa 1. Alternando movimento e pausa, cada passo sai por 4 em vez
        // de 6, o que estica bastante o tempo de vida do agente.
        if (turno % 2 != 0) {
            para();
            return;
        }

        if (!podeMoverPara(getDirecao())) {
            setDirecao(geraDirecaoAleatoria());
        }
    }

    /**
     * Caminha em direcao a uma coordenada do mapa. Tenta primeiro o eixo com
     * maior diferenca; se estiver bloqueado, tenta o outro.
     */
    private void moverParaCoordenada(int destinoX, int destinoY) {
        int dx = destinoX - getX();
        int dy = destinoY - getY();

        if (dx == 0 && dy == 0) {
            para();
            return;
        }

        int primeiraDirecao;
        int segundaDirecao;

        if (Math.abs(dx) >= Math.abs(dy)) {
            primeiraDirecao = dx > 0 ? DIREITA : ESQUERDA;
            segundaDirecao = dy > 0 ? BAIXO : CIMA;
        } else {
            primeiraDirecao = dy > 0 ? BAIXO : CIMA;
            segundaDirecao = dx > 0 ? DIREITA : ESQUERDA;
        }

        if (podeMoverPara(primeiraDirecao)) {
            setDirecao(primeiraDirecao);
        } else if ((dx != 0 || dy != 0) && podeMoverPara(segundaDirecao)) {
            setDirecao(segundaDirecao);
        } else {
            para();
        }
    }

    private int direcaoOposta(int direcao) {
        switch (direcao) {
            case DIREITA:
                return ESQUERDA;
            case ESQUERDA:
                return DIREITA;
            case CIMA:
                return BAIXO;
            case BAIXO:
                return CIMA;
            default:
                return DIREITA;
        }
    }

    private int distancia(int x, int y) {
        return distancia(getX(), getY(), x, y);
    }

    private int distancia(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
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