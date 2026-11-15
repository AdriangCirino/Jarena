package br.uffs.cc.jarena;

public class AgenteGuarana extends Agente {

    private int turno;
    private int indiceEquipe;
    private boolean recebeuHandshake;
    private boolean inicializado;

    private int setorMinX;
    private int setorMaxX;
    private int setorMinY;
    private int setorMaxY;
    private int dirH;
    private int dirV;

    private int cogumeloX = -1;
    private int cogumeloY = -1;
    private int validadeCogumelo;

    private boolean recebendoEnergia;
    private int turnosNoCogumelo;
    private int turnosSemEnergia;
    private int ultimoEnvioEnergia = -100;

    private boolean emCombate;
    private int energiaInimigo;
    private int turnosCombate;
    private int turnosSemDano;
    private boolean jaDividiu;

    private int fugaTicks;
    private int dirFuga;

    public AgenteGuarana(Integer x, Integer y, Integer energia) {
        super(x, y, energia);
    }

    @Override
    public void pensa() {
        turno++;
        atualizaTemporizadores();

        if (turno == 1) {
            enviaMensagem("H|" + getId());
            setDirecao(DIREITA);
            return;
        }

        if (!inicializado) {
            inicializaSetor();
            inicializado = true;
        }

        if (fugaTicks > 0) {
            executaFuga();
            return;
        }

        if (emCombate) {
            if (deveDividirCombate()) {
                jaDividiu = true;
                divide();
                return;
            }
            if (energiaInimigo > getEnergia() + 150 && turnosCombate >= 2) {
                iniciaFuga();
                return;
            }
            if (podeMoverPara(dirH)) {
                setDirecao(dirH);
            }
            return;
        }

        if (getEnergia() >= 1550 && podeDividir()) {
            divide();
            return;
        }

        if (recebendoEnergia) {
            turnosNoCogumelo++;
            if (turnosNoCogumelo % 4 == 0) {
                andaUmPassoQualquer();
            } else {
                para();
            }
            return;
        }
        turnosNoCogumelo = 0;

        if (validadeCogumelo > 0 && distancia(cogumeloX, cogumeloY) <= 180) {
            moverPara(cogumeloX, cogumeloY);
            return;
        }

        int centroX = (setorMinX + setorMaxX) / 2;
        int centroY = (setorMinY + setorMaxY) / 2;
        if (!estaNoSetor()) {
            if (turno % 3 == 0) {
                para();
            } else {
                moverPara(centroX, centroY);
            }
            return;
        }

        patrulhaSetor();
    }

    private void inicializaSetor() {
        int base = recebeuHandshake ? indiceEquipe : getId();
        int idx = Math.abs(base) % 15;
        int col = idx % 5;
        int lin = idx / 5;

        setorMinX = col * Constants.LARGURA_MAPA / 5;
        setorMaxX = (col + 1) * Constants.LARGURA_MAPA / 5;
        setorMinY = lin * Constants.ALTURA_MAPA / 3;
        setorMaxY = (lin + 1) * Constants.ALTURA_MAPA / 3;

        dirH = (idx % 2 == 0) ? DIREITA : ESQUERDA;
        dirV = (idx % 4 < 2) ? BAIXO : CIMA;
        setDirecao(dirH);
    }

    private boolean estaNoSetor() {
        return getX() >= setorMinX + 15 && getX() <= setorMaxX - 15
                && getY() >= setorMinY + 15 && getY() <= setorMaxY - 15;
    }

    private void patrulhaSetor() {
        if (turno % 3 == 0) {
            para();
            return;
        }

        int proxX = getX() + (dirH == DIREITA ? 5 : -5);
        if (proxX < setorMinX + 15 || proxX > setorMaxX - 15 || !podeMoverPara(dirH)) {
            dirH = direcaoOposta(dirH);

            int proxY = getY() + (dirV == BAIXO ? 15 : -15);
            if (proxY < setorMinY + 15 || proxY > setorMaxY - 15 || !podeMoverPara(dirV)) {
                dirV = direcaoOposta(dirV);
            }
            if (podeMoverPara(dirV)) {
                setDirecao(dirV);
                return;
            }
        }

        if (podeMoverPara(dirH)) {
            setDirecao(dirH);
        } else {
            andaUmPassoQualquer();
        }
    }

    private void moverPara(int destinoX, int destinoY) {
        int dx = destinoX - getX();
        int dy = destinoY - getY();

        if (Math.abs(dx) <= 5 && Math.abs(dy) <= 5) {
            andaUmPassoQualquer();
            return;
        }

        int prim = Math.abs(dx) >= Math.abs(dy)
                ? (dx > 0 ? DIREITA : ESQUERDA)
                : (dy > 0 ? BAIXO : CIMA);
        int seg = Math.abs(dx) >= Math.abs(dy)
                ? (dy > 0 ? BAIXO : CIMA)
                : (dx > 0 ? DIREITA : ESQUERDA);

        if (podeMoverPara(prim)) {
            setDirecao(prim);
        } else if ((dx != 0 && dy != 0) && podeMoverPara(seg)) {
            setDirecao(seg);
        } else {
            andaUmPassoQualquer();
        }
    }

    private void andaUmPassoQualquer() {
        int[] dirs = {DIREITA, BAIXO, ESQUERDA, CIMA};
        for (int i = 0; i < 4; i++) {
            int d = dirs[(turno + i) % 4];
            if (podeMoverPara(d)) {
                setDirecao(d);
                return;
            }
        }
        para();
    }

    private void iniciaFuga() {
        dirFuga = direcaoOposta(getDirecao());
        if (!podeMoverPara(dirFuga)) {
            dirFuga = direcaoOposta(dirH);
        }
        fugaTicks = 6;
        emCombate = false;
        turnosCombate = 0;
        jaDividiu = false;
        setDirecao(dirFuga);
    }

    private void executaFuga() {
        if (podeMoverPara(dirFuga)) {
            setDirecao(dirFuga);
        } else {
            andaUmPassoQualquer();
        }
        fugaTicks--;
    }

    private boolean deveDividirCombate() {
        return !jaDividiu
                && turnosCombate >= 2
                && getEnergia() >= 700
                && getEnergia() <= 1300
                && energiaInimigo <= 350
                && getEnergia() >= energiaInimigo * 2 + 100
                && podeDividir();
    }

    private void atualizaTemporizadores() {
        if (recebendoEnergia && ++turnosSemEnergia > 2) {
            recebendoEnergia = false;
        }
        if (emCombate && ++turnosSemDano > 2) {
            emCombate = false;
            turnosCombate = 0;
            jaDividiu = false;
        }
        if (validadeCogumelo > 0) {
            validadeCogumelo--;
        }
    }

    @Override
    public void recebeuEnergia() {
        recebendoEnergia = true;
        turnosSemEnergia = 0;
        cogumeloX = getX();
        cogumeloY = getY();
        validadeCogumelo = 6;

        if (turno - ultimoEnvioEnergia >= 6) {
            ultimoEnvioEnergia = turno;
            enviaMensagem("C|" + getX() + "|" + getY());
        }
    }

    @Override
    public void tomouDano(int energiaRestanteInimigo) {
        if (!emCombate) {
            turnosCombate = 0;
            jaDividiu = false;
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
        jaDividiu = false;
    }

    @Override
    public void recebeuMensagem(String msg) {
        if (msg == null) return;
        try {
            String[] p = msg.split("\\|");
            if (p[0].equals("H") && p.length == 2) {
                recebeuHandshake = true;
                int idAliado = Integer.parseInt(p[1]);
                if (idAliado < getId()) indiceEquipe++;
            } else if (p[0].equals("C") && p.length == 3) {
                cogumeloX = Integer.parseInt(p[1]);
                cogumeloY = Integer.parseInt(p[2]);
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