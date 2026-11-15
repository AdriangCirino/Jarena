package br.uffs.cc.jarena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    private int combateX;
    private int combateY;
    private int energiaInimigo;
    private int turnosCombate;
    private int turnosSemDano;
    private int validadeCombate;
    private boolean jaDividiuNoCombate;

    private int recuoTicks;
    private int direcaoRecuo;

    private final Map<Integer, int[]> aliados = new HashMap<Integer, int[]>();

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
        removeAliadosAntigos();

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

        if (turno % 8 == 0) {
            enviaMensagem("P|" + getId() + "|" + getX() + "|" + getY() + "|" + getEnergia());
        }

        if (emCombate) {
            if (deveDividir()) {
                jaDividiuNoCombate = true;
                divide();
                return;
            }
            if (deveRecuar()) {
                iniciaRecuo();
            } else {
                para();
                return;
            }
        }

        if (recuoTicks > 0) {
            executaRecuo();
            return;
        }

        if (validadeCombate > 0 && (papel == COMBATENTE || distancia(combateX, combateY) <= 100)) {
            moverParaCoordenada(combateX, combateY);
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
        if (validadeCombate > 0) validadeCombate--;
    }

    private void removeAliadosAntigos() {
        ArrayList<Integer> remover = new ArrayList<Integer>();
        for (Map.Entry<Integer, int[]> entrada : aliados.entrySet()) {
            if (turno - entrada.getValue()[3] > 15) remover.add(entrada.getKey());
        }
        for (Integer id : remover) aliados.remove(id);
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

    private boolean deveRecuar() {
        int qtd = 0;
        for (int[] info : aliados.values()) {
            if (turno - info[3] <= 10 && distancia(info[0], info[1], combateX, combateY) <= 100) qtd++;
        }
        return turnosCombate >= 8 && energiaInimigo > getEnergia() + 120 && qtd == 0;
    }

    private void iniciaRecuo() {
        direcaoRecuo = direcaoOposta(getDirecao());
        if (!podeMoverPara(direcaoRecuo)) direcaoRecuo = direcaoOposta(direcaoHorizontal);
        recuoTicks = 8;
        emCombate = false;
        validadeCombate = 0;
        turnosCombate = 0;
        jaDividiuNoCombate = false;
        setDirecao(direcaoRecuo);
        enviaMensagem("R|" + getId() + "|" + getX() + "|" + getY());
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
        if (turno - ultimoEnvioCogumelo >= 5) {
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
        combateX = getX();
        combateY = getY();
        energiaInimigo = energiaRestanteInimigo;
        if (turnosCombate == 1 || turnosCombate % 3 == 1) {
            enviaMensagem("B|" + getId() + "|" + getX() + "|" + getY() + "|" + energiaRestanteInimigo);
        }
    }

    @Override
    public void ganhouCombate() {
        emCombate = false;
        validadeCombate = 0;
        turnosCombate = 0;
        jaDividiuNoCombate = false;
    }

    @Override
    public void recebeuMensagem(String msg) {
        if (msg == null) return;
        String[] p = msg.split("\\|");
        try {
            if (p[0].equals("P") && p.length == 5) {
                int id = Integer.parseInt(p[1]);
                if (id != getId()) {
                    aliados.put(id, new int[]{Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]), turno});
                }
            } else if (p[0].equals("C") && p.length == 3 && !emCombate && getEnergia() < 1800) {
                alvoCogumeloX = Integer.parseInt(p[1]);
                alvoCogumeloY = Integer.parseInt(p[2]);
                validadeCogumelo = 18;
            } else if (p[0].equals("B") && p.length == 5 && getEnergia() >= 220) {
                combateX = Integer.parseInt(p[1]);
                combateY = Integer.parseInt(p[2]);
                energiaInimigo = Integer.parseInt(p[4]);
                validadeCombate = 12;
            }
        } catch (Exception ignored) {
        }
    }

    private int distancia(int x, int y) { return distancia(getX(), getY(), x, y); }
    private int distancia(int x1, int y1, int x2, int y2) { return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2)); }

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