package br.uffs.cc.jarena;

public class AgenteGuarana extends Agente {

    private static final int EXPLORADOR = 0;
    private static final int COMBATENTE = 1;
    private static final int COLETOR = 2;
    private static final int ORCAMENTO_EXPLORACAO = 50;

    private final int papel;
    private final int setorMinX;
    private final int setorMaxX;
    private final int setorMinY;
    private final int setorMaxY;
    private final int centroSetorX;
    private final int centroSetorY;
    private final int energiaInicial;

    private int turno;
    private boolean primeiroTurno = true;
    private boolean aposentado;

    private boolean varreduraHorizontal = true;
    private int direcaoHorizontal;
    private int direcaoVertical;
    private int passosVerticais;

    private boolean recebendoEnergia;
    private int turnosSemEnergia;
    private int ultimoEnvioCogumelo = -100;

    private int alvoCogumeloX = -1;
    private int alvoCogumeloY = -1;
    private int validadeCogumelo;

    private boolean emCombate;
    private int energiaInimigo;
    private int turnosCombate;
    private int turnosSemDano;
    private boolean jaDividiuNoCombate;

    private int recuoTicks;
    private int direcaoRecuo;

    public AgenteGuarana(Integer x, Integer y, Integer energia) {
        super(x, y, energia);

        papel = getId() % 5 == 0 ? EXPLORADOR
                : getId() % 5 == 1 ? COMBATENTE : COLETOR;

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

        energiaInicial = getEnergia();
    }

    @Override
    public void pensa() {
        turno++;
        atualizaTemporizadores();

        if (primeiroTurno) {
            primeiroTurno = false;
            para();
            return;
        }

        if (!aposentado && getEnergia() < energiaInicial - ORCAMENTO_EXPLORACAO) {
            aposentado = true;
        }

        if (aposentado) {
            para();
            return;
        }

        if (emCombate) {
            if (deveDividir()) {
                jaDividiuNoCombate = true;
                divide();
                return;
            }
            if (turnosCombate >= 4 && energiaInimigo > getEnergia() + 120) {
                iniciaRecuo();
                return;
            }
            para();
            return;
        }

        if (recuoTicks > 0) {
            executaRecuo();
            return;
        }

        if (recebendoEnergia) {
            para();
            return;
        }

        if (validadeCogumelo > 0 && (papel != EXPLORADOR || distancia(alvoCogumeloX, alvoCogumeloY) <= 70)) {
            moverParaCoordenada(alvoCogumeloX, alvoCogumeloY);
            return;
        }

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

    private void atualizaTemporizadores() {
        if (recebendoEnergia && ++turnosSemEnergia > 2) {
            recebendoEnergia = false;
        }
        if (emCombate && ++turnosSemDano > 2) {
            emCombate = false;
            turnosCombate = 0;
            jaDividiuNoCombate = false;
        }
        if (validadeCogumelo > 0) validadeCogumelo--;
    }

    private boolean estaDentroDoSetor() {
        return getX() >= setorMinX + 10 && getX() <= setorMaxX - 10
                && getY() >= setorMinY + 10 && getY() <= setorMaxY - 10;
    }

    private void fazVarredura() {
        if (varreduraHorizontal) {
            int proximoX = getX() + (direcaoHorizontal == DIREITA ? 5 : -5);
            if (proximoX < setorMinX + 10 || proximoX > setorMaxX - 10 || !podeMoverPara(direcaoHorizontal)) {
                varreduraHorizontal = false;
                passosVerticais = 0;
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
        int primeira = Math.abs(dx) >= Math.abs(dy) ? (dx > 0 ? DIREITA : ESQUERDA) : (dy > 0 ? BAIXO : CIMA);
        int segunda = Math.abs(dx) >= Math.abs(dy) ? (dy > 0 ? BAIXO : CIMA) : (dx > 0 ? DIREITA : ESQUERDA);
        if (podeMoverPara(primeira)) setDirecao(primeira);
        else if ((dx != 0 || dy != 0) && podeMoverPara(segunda)) setDirecao(segunda);
        else para();
    }

    private boolean deveDividir() {
        return !jaDividiuNoCombate && turnosCombate >= 2
                && getEnergia() >= 700 && getEnergia() <= 1300
                && energiaInimigo <= 350 && getEnergia() >= energiaInimigo * 2 + 100
                && podeDividir();
    }

    private void iniciaRecuo() {
        direcaoRecuo = direcaoOposta(getDirecao());
        if (!podeMoverPara(direcaoRecuo)) direcaoRecuo = direcaoOposta(direcaoHorizontal);
        recuoTicks = 6;
        emCombate = false;
        turnosCombate = 0;
        jaDividiuNoCombate = false;
        setDirecao(direcaoRecuo);
    }

    private void executaRecuo() {
        if (podeMoverPara(direcaoRecuo)) setDirecao(direcaoRecuo);
        else para();
        recuoTicks--;
    }

    @Override
    public void recebeuEnergia() {
        recebendoEnergia = true;
        turnosSemEnergia = 0;
        if (turno - ultimoEnvioCogumelo >= 6) {
            ultimoEnvioCogumelo = turno;
            enviaMensagem("C|" + getX() + "|" + getY());
        }
    }

    @Override
    public void tomouDano(int energiaRestanteInimigo) {
        if (!emCombate) {
            turnosCombate = 0;
            jaDividiuNoCombate = false;
        }
        emCombate = true;
        turnosSemDano = 0;
        turnosCombate++;
        energiaInimigo = energiaRestanteInimigo;
    }

    @Override
    public void ganhouCombate() {
        emCombate = false;
        turnosCombate = 0;
        jaDividiuNoCombate = false;
    }

    @Override
    public void recebeuMensagem(String msg) {
        if (msg == null) return;
        try {
            String[] p = msg.split("\\|");
            if (p[0].equals("C") && p.length == 3 && !emCombate && getEnergia() < 1800) {
                alvoCogumeloX = Integer.parseInt(p[1]);
                alvoCogumeloY = Integer.parseInt(p[2]);
                validadeCogumelo = 12;
            }
        } catch (Exception ignored) {
        }
    }

    private int distancia(int x, int y) { return Math.max(Math.abs(getX() - x), Math.abs(getY() - y)); }

    private int direcaoOposta(int d) {
        switch (d) {
            case DIREITA: return ESQUERDA;
            case ESQUERDA: return DIREITA;
            case CIMA: return BAIXO;
            case BAIXO: return CIMA;
            default: return DIREITA;
        }
    }

    @Override
    public String getEquipe() { return "Guarana"; }
}