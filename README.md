# Siecie Sterowane Programowo

Projekt ma na celu implementację mechanizmu obrony przeciwko atakowi DoS/DDoS z wykorzystaniem sterownika Floodlight.

## Zespół projektowy
* Kulig Sebastian
* Mirowska Diana
* Wnęk Karol


## Użyta topologia
Testowa sieć składa się z czterech hostów, które przeprowadzają atak, jednego serwera, na którym uruchomiono prosty serwer http. Całość jest połączona za pomocą jednego przełącznika komunikującego się z kontrolerem.
![topologia](./images/topo.png)

przygotowana topologia  znajduje się w pliku ....

## Schemat działania

Całość została podzielona na trzy moduły funkcjonalne:
1. Zbieranie statystyk z przełącznika - klasa ....
2. Podjęcie decyzji o usunięciu/zachowaniu danego przepływu na podstawie uzyskanych wyników pomiarów - klasa ....
3. Wdrożenie ustalonej strategii - klasa .... 

## Opis poszczególnych plików

## Sposób uruchomienia

## Przykładowy wynik
Sumaryczna imość pakietów wejściowych do poszzcególnych portów przełącznika z wyłączonym mechanizmem odrzucania ruchu:
![no_discarding](./images/attack.png)
Ten sam scenariusz testowy, ale z progiem decyzyjnym ustalonym na wartość 100 pakietów na sekundę:
![no_discarding](./images/attack2.png)

