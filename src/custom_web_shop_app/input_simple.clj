(ns custom-web-shop-app.input-simple)

; Products
(def products
  ["Apple" "Avocado" "Banana" "Pear" "Cherry"])

; Stores
(def stores
  ["Aldi" "Carrefour" "Colruyt" "Delhaize" "Lidl"])

; The price of each item in each store.
;
; An apple costs €0.25 in Aldi, €0.30 in Carrefour, etc.
(def prices
  ; Aldi Carr Colr Delh Lidl
  [[0.25 0.30 0.28 0.29 0.27] ; Apple
   [1.37 1.20 1.25 1.20 1.32] ; Avocado
   [0.41 0.35 0.35 0.36 0.45] ; Banana
   [0.19 0.21 0.19 0.25 0.18] ; Pear
   [0.23 0.11 0.29 0.27 0.12] ; Cherry
  ])

; The number of items left of each item in each store.
;
; E.g. there are 15 apples left at Aldi, 25 apples at Carrefour, etc.
(def stock
  ; Aldi Carr Colr Delh Lidl
  [[  15   25   30   29   15] ; Apple
   [   5    7    6   10    2] ; Avocado
   [   2   10   20   17    8] ; Banana
   [  25   17   31   18   20] ; Pear
   [  5   2   1   4   3] ; Cherry
  ])

; Customers and the products they want to buy.
;
; E.g. customer 0 wants to buy 5 bananas and 7 apples.
(def customers
  [
   {:id 0 :products [["Banana" 5] ["Apple" 7]]}
   {:id 1 :products [["Banana" 7] ["Apple" 9]]}
   {:id 2 :products [["Pear" 15]]}
   {:id 3 :products [["Apple" 15] ["Avocado" 4] ["Banana" 7] ["Pear" 2]]}
   {:id 4 :products [["Apple" 5] ["Apple" 2]]}
   {:id 5 :products [["Banana" 4]]}
   {:id 6 :products [["Avocado" 2] ["Apple" 1]]}
   {:id 7 :products [["Banana" 3] ["Apple" 5] ["Pear" 7]]}
   {:id 8 :products [["Pear" 1] ["Banana" 4]]}
   {:id 9 :products [["Apple" 6] ["Pear" 4] ["Avocado" 3]]}
  ;;  {:id 10 :products [["Banana" 5] ["Apple" 7]]}
  ;;  {:id 11 :products [["Banana" 7] ["Apple" 9]]}
  ;;  {:id 12 :products [["Pear" 15]]}
  ;;  {:id 13 :products [["Apple" 15] ["Avocado" 4] ["Banana" 7] ["Pear" 2]]}
  ;;  {:id 14 :products [["Apple" 5] ["Apple" 2]]}
  ;;  {:id 15 :products [["Banana" 4]]}
  ;;  {:id 16 :products [["Avocado" 2] ["Apple" 1]]}
  ;;  {:id 17 :products [["Banana" 3] ["Apple" 5] ["Pear" 7]]}
  ;;  {:id 18 :products [["Pear" 1] ["Banana" 4]]}
  ;;  {:id 19 :products [["Apple" 6] ["Pear" 4] ["Avocado" 3]]}
  ;;  {:id 20 :products [["Banana" 5] ["Apple" 7]]}
  ;;  {:id 21 :products [["Banana" 7] ["Apple" 9]]}
  ;;  {:id 22 :products [["Pear" 15]]}
  ;;  {:id 23 :products [["Apple" 15] ["Avocado" 4] ["Banana" 7] ["Pear" 2]]}
  ;;  {:id 24 :products [["Apple" 5] ["Apple" 2]]}
  ;;  {:id 25 :products [["Banana" 4]]}
  ;;  {:id 26 :products [["Avocado" 2] ["Apple" 1]]}
  ;;  {:id 27 :products [["Banana" 3] ["Apple" 5] ["Pear" 7]]}
  ;;  {:id 28 :products [["Pear" 1] ["Banana" 4]]}
  ;;  {:id 29 :products [["Apple" 6] ["Pear" 4] ["Avocado" 3]]}
  ;;  {:id 30 :products [["Banana" 5] ["Apple" 7]]}
  ;;  {:id 31 :products [["Banana" 7] ["Apple" 9]]}
  ;;  {:id 32 :products [["Pear" 15]]}
  ;;  {:id 33 :products [["Apple" 15] ["Avocado" 4] ["Banana" 7] ["Pear" 2]]}
  ;;  {:id 34 :products [["Apple" 5] ["Apple" 2]]}
  ;;  {:id 35 :products [["Banana" 4]]}
  ;;  {:id 36 :products [["Avocado" 2] ["Apple" 1]]}
  ;;  {:id 37 :products [["Banana" 3] ["Apple" 5] ["Pear" 7]]}
  ;;  {:id 38 :products [["Pear" 1] ["Banana" 4]]}
  ;;  {:id 39 :products [["Apple" 6] ["Pear" 4] ["Avocado" 3]]}
  ;;  {:id 40 :products [["Banana" 5] ["Apple" 7]]}
  ;;  {:id 41 :products [["Banana" 7] ["Apple" 9]]}
  ;;  {:id 42 :products [["Pear" 15]]}
  ;;  {:id 43 :products [["Apple" 15] ["Avocado" 4] ["Banana" 7] ["Pear" 2]]}
  ;;  {:id 44 :products [["Apple" 5] ["Apple" 2]]}
  ;;  {:id 45 :products [["Banana" 4]]}
  ;;  {:id 46 :products [["Avocado" 2] ["Apple" 1]]}
  ;;  {:id 47 :products [["Banana" 3] ["Apple" 5] ["Pear" 7]]}
  ;;  {:id 48 :products [["Pear" 1] ["Banana" 4]]}
  ;;  {:id 49 :products [["Apple" 6] ["Pear" 4] ["Avocado" 3]]}
  ;;  {:id 50 :products [["Banana" 5] ["Apple" 7]]}
  ;;  {:id 51 :products [["Banana" 7] ["Apple" 9]]}
  ;;  {:id 52 :products [["Pear" 15]]}
  ;;  {:id 53 :products [["Apple" 15] ["Avocado" 4] ["Banana" 7] ["Pear" 2]]}
  ;;  {:id 54 :products [["Apple" 5] ["Apple" 2]]}
  ;;  {:id 55 :products [["Banana" 4]]}
  ;;  {:id 56 :products [["Avocado" 2] ["Apple" 1]]}
  ;;  {:id 57 :products [["Banana" 3] ["Apple" 5] ["Pear" 7]]}
  ;;  {:id 58 :products [["Pear" 1] ["Banana" 4]]}
  ;;  {:id 59 :products [["Apple" 6] ["Pear" 4] ["Avocado" 3]]}
  ;;  {:id 60 :products [["Banana" 5] ["Apple" 7]]}
  ;;  {:id 61 :products [["Banana" 7] ["Apple" 9]]}
  ;;  {:id 62 :products [["Pear" 15]]}
  ;;  {:id 63 :products [["Apple" 15] ["Avocado" 4] ["Banana" 7] ["Pear" 2]]}
  ;;  {:id 64 :products [["Apple" 5] ["Apple" 2]]}
  ;;  {:id 65 :products [["Banana" 4]]}
  ;;  {:id 66 :products [["Avocado" 2] ["Apple" 1]]}
  ;;  {:id 67 :products [["Banana" 3] ["Apple" 5] ["Pear" 7]]}
  ;;  {:id 68 :products [["Pear" 1] ["Banana" 4]]}
  ;;  {:id 69 :products [["Apple" 6] ["Pear" 4] ["Avocado" 3]]}
  ])

; Time in milliseconds between sales periods
(def TIME_BETWEEN_SALES 50)
; Time in milliseconds of sales period
(def TIME_OF_SALES 10)
