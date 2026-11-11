<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHSnow

*Fait tomber des flocons de neige autour des joueurs dans les mondes choisis, uniquement à ciel ouvert.*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

Fait tomber des flocons de neige autour des joueurs dans les mondes choisis, uniquement à ciel ouvert.

- À chaque vague, des flocons sont tirés au hasard dans un cube autour de chaque joueur et envoyés à lui seul. Les particules utilisées alternent (cire, flocon, baguette de l'End par défaut).
- Un flocon n'est envoyé que s'il démarre au-dessus du toit de sa colonne : la neige ne tombe pas à l'intérieur des bâtiments. Les blocs listés dans `roof.ignored-blocks` (les barrières par défaut) ne comptent pas comme un toit.
- La neige s'arrête dans un monde où il pleut déjà.
- Chaque joueur peut couper la neige pour lui seul avec `/snow`. Son choix est gardé dans ses données de joueur.
- Le thread principal ne fait que lire la météo, la position des joueurs et la hauteur des toits, gardée en mémoire par chunk. Le tirage des flocons et l'envoi des paquets se font hors du thread principal.

Utilisé sur : **Opale**.

## Commandes

| Commande | Description | Permission |
|---|---|---|
| `/snow` ou `/snow toggle` | Active ou coupe la neige pour soi. | `shsnow.toggle` |
| `/shsnow reload` | Recharge `config.yml` et `messages.yml` ; si un fichier est illisible, la configuration précédente reste active. Marche aussi depuis la console. | `shsnow.admin.reload` |

`/shsnow` et `/shsnow toggle` font la même chose que `/snow` pour qui n'a pas de permission d'administration ; avec une permission d'administration, `/shsnow` affiche l'usage. L'ancienne forme `/snow reload` reste acceptée.

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shsnow.toggle` | Utiliser /snow pour activer ou couper la neige. | tout le monde |
| `shsnow.admin` | Toutes les sous-commandes de /shsnow. | `op` |
| `shsnow.admin.reload` | Utiliser /shsnow reload. | `op` |

## Configuration

Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHSnow/
├── <a href="src/main/resources/config.yml">config.yml</a>     mondes, particules, densité, rayon, vitesse, toits
└── <a href="src/main/resources/messages.yml">messages.yml</a>
</pre>

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
