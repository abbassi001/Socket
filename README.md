# Explication du Code du Jeu de Morpion

Ce document explique la structure et le fonctionnement des différentes classes du jeu de Morpion en réseau.

## Vue d'ensemble de l'architecture

Le jeu utilise une architecture client-serveur :

1. **Le serveur** (`MorpionServer`) :
   - Gère les connexions des clients
   - Implémente la logique du jeu
   - Met en relation les joueurs ou propose une IA
   - Valide les coups joués

2. **Le client** (`MorpionClientFX`) :
   - Fournit une interface graphique JavaFX
   - Communique avec le serveur
   - Affiche l'état actuel du jeu
   - Permet au joueur d'interagir

3. **Le lanceur** (`MorpionLauncher`) :
   - Offre une interface pour lancer facilement le serveur et les clients
   - Simplifie le démarrage du jeu

## Explication des Classes Principales

### 1. MorpionServer

Cette classe représente le serveur du jeu et gère toute la logique côté serveur.

#### Classes internes :
- **Game** : Représente une partie entre deux joueurs ou contre l'IA
- **PlayerHandler** : Gère la connexion et la communication avec chaque client

#### Fonctionnalités principales :
- Accepte les connexions des clients
- Permet aux joueurs de choisir entre jouer contre l'IA ou un autre joueur
- Met en relation les joueurs en attente
- Gère l'état du plateau de jeu
- Valide les coups des joueurs
- Vérifie les conditions de victoire ou de match nul
- Propose des rematches

#### Fonctionnement de l'IA :
L'IA implémente une stratégie simple mais efficace :
1. Essayer de gagner si possible
2. Bloquer l'adversaire s'il est sur le point de gagner
3. Jouer au centre si disponible
4. Jouer dans un coin disponible
5. Jouer sur une case disponible quelconque

### 2. MorpionClientFX

Cette classe est le client graphique du jeu, développé avec JavaFX.

#### Fonctionnalités principales :
- Interface utilisateur graphique complète
- Dialogue de configuration pour choisir le mode et la connexion
- Affichage du plateau de jeu
- Gestion des événements (clics sur les cases)
- Communication avec le serveur
- Mode hors-ligne contre l'ordinateur
- Notifications visuelles (tour actuel, victoire, défaite, match nul)

#### Structure :
- Initialisation de l'interface utilisateur
- Connexion au serveur (mode multijoueur)
- Gestion des événements et des messages du serveur
- Analyse et affichage de l'état du plateau
- Gestion des fins de partie et des notifications

### 3. MorpionLauncher

Cette classe sert de point d'entrée pour démarrer facilement le jeu.

#### Fonctionnalités principales :
- Interface graphique pour lancer le serveur et/ou les clients
- Option pour lancer un second client sur la même machine
- Configuration simplifiée pour jouer à deux en local
- Indication visuelle de l'état du serveur
- Options pour choisir entre mode solo et multijoueur

## Protocole de Communication

La communication entre le client et le serveur utilise un protocole texte simple :

### Messages du client vers le serveur :
- `AI` : Demande de jouer contre l'IA
- `WAIT` : Demande d'attendre un autre joueur
- `REPLAY` : Demande une nouvelle partie
- `QUIT` : Quitte la partie
- `XY` (où X et Y sont des chiffres) : Joue un coup à la position (X,Y)

### Messages du serveur vers le client :
- Messages d'information (début de partie, tour actuel, etc.)
- Représentation textuelle du plateau
- Notifications de fin de partie (victoire, défaite, match nul)
- Demandes d'action

## Fonctionnement du Jeu

1. **Démarrage** :
   - Le serveur est lancé et attend les connexions
   - Les clients se connectent et choisissent leur mode de jeu

2. **Phase de jeu** :
   - Les joueurs jouent à tour de rôle
   - Le serveur valide chaque coup et met à jour l'état du jeu
   - Le plateau est affiché après chaque coup

3. **Fin de partie** :
   - Lorsqu'un joueur gagne ou que le plateau est plein
   - Les joueurs sont notifiés du résultat
   - Option de rejouer est proposée

## Structure des Classes et Relations

```
MorpionLauncher
  |
  ├── démarre ──> MorpionServer
  |                  |
  |                  ├── Game (partie de jeu)
  |                  └── PlayerHandler (gestion des clients)
  |
  └── démarre ──> MorpionClientFX
                    |
                    └── Interface graphique JavaFX
```

## Gestion des Événements

### Côté Serveur :
- Acceptation des connexions clients
- Traitement des messages entrants
- Validation des coups
- Mise à jour de l'état du jeu
- Envoi des notifications aux clients

### Côté Client :
- Événements d'interface utilisateur (clics)
- Envoi des actions au serveur
- Réception et traitement des messages du serveur
- Mise à jour de l'interface graphique

## Aspects Techniques

- Utilisation de threads pour gérer plusieurs clients simultanément
- Synchronisation pour éviter les problèmes de concurrence
- Gestion des erreurs et des déconnexions
- Interface utilisateur réactive avec JavaFX
- Encodage UTF-8 pour supporter les émojis dans les messages