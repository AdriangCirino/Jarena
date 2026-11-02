package br.uffs.cc.jarena;

public class AgenteGuarana extends Agente {

    private final int setorMinX;
    private final int setorMaxX;
    private final int setorMinY;
    private final int setorMaxY;
    private final int centroSetorX;
    private final int centroSetorY;

    private int turno;
    private boolean primeiroTurno = true;

    private boolean varreduraHorizontal = true;
    private int direcaoHorizontal;
    private int direcaoVertical;
    private int passosVerticais;

    public AgenteGuarana(Integer x, Integer y, Integer energia) {
        super(x, y, energia);

        // O mapa e dividido em 15 setores: 3 colunas por 5 linhas.
        int coluna = (getId() % 3);
        int linha = (getId() / 3) % 5;

        setorMinX = coluna * Constants.LARGURA_MAPA / 3;
        setorMaxX = (coluna + 1) * Constants.LARGURA_MAPA / 3;
        setorMinY = linha * Constants.ALTURA_MAPA / 5;
        setorMaxY = (linha + 1) * Constants.ALTURA_MAPA / 5;

        centroSetorX = (setorMinX + setorMaxX) / 2;
        centroSetorY = (setorMinY + setorMaxY) / 2;

        direcaoHorizontal = getId() % 2 == 0 ? DIREITA : ESQUERDA;
        direcaoVertical = getId() % 4 < 2 ? BAIXO : CIMA;
        setDirecao(direcaoHorizontal);
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

        if (!estaDentroDoSetor()) {
            moverParaCoordenada(centroSetorX, centroSetorY);
            return;
        }

        fazVarredura();
    }

    private boolean estaDentroDoSetor() {
        return getX() >= setorMinX + 10 && getX() <= setorMaxX - 10
                && getY() >= setorMinY + 10 && getY() <= setorMaxY - 10;
    }

    /**
     * Varredura em serpentina: anda na horizontal ate o limite da faixa,
     * desce alguns passos na vertical e inverte o sentido horizontal. Cobre
     * o setor de forma previsivel, sem depender de direcoes aleatorias.
     */
    private void fazVarredura() {
        if (varreduraHorizontal) {
            int proximoX = getX() + (direcaoHorizontal == DIREITA ? 5 : -5);

            if (proximoX < setorMinX + 10 || proximoX > setorMaxX - 10
                    || !podeMoverPara(direcaoHorizontal)) {
                varreduraHorizontal = false;
                passosVerticais = 0;
                if (!podeMoverPara(direcaoVertical)
                        || getY() + (direcaoVertical == BAIXO ? 5 : -5) < setorMinY + 10
                        || getY() + (direcaoVertical == BAIXO ? 5 : -5) > setorMaxY - 10) {
                    direcaoVertical = direcaoOposta(direcaoVertical);
                }
                para();
                return;
            }

            setDirecao(direcaoHorizontal);
            return;
        }

        int proximoY = getY() + (direcaoVertical == BAIXO ? 5 : -5);

        if (passosVerticais < 6 && proximoY >= setorMinY + 10
                && proximoY <= setorMaxY - 10 && podeMoverPara(direcaoVertical)) {
            setDirecao(direcaoVertical);
            passosVerticais++;
            return;
        }

        varreduraHorizontal = true;
        direcaoHorizontal = direcaoOposta(direcaoHorizontal);
        passosVerticais = 0;
        para();
    }

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

    private int direcaoParaCentro() {
        int dx = centroSetorX - getX();
        int dy = centroSetorY - getY();

        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx >= 0 ? DIREITA : ESQUERDA;
        }
        return dy >= 0 ? BAIXO : CIMA;
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