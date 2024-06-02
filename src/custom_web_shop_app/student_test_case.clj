(ns custom-web-shop-app.student-test-case)

; Products
(def products
  ["Apple" "Avocado" "Banana" "Pear"])

; Stores
(def stores
  ["Aldi"])

; The price of each item in each store.
;
; An apple costs €0.25 in Aldi, etc.
(def prices
  ; Aldi
  [[0.25] ; Apple
   [1.37] ; Avocado
   [0.41] ; Banana
   [0.19] ; Pear
   ])

; The number of items left of each item in each store.
;
; One store with 5 apples left, 5 avocados left, etc.
(def stock
  ; Aldi
  [[5] ; Apple
   [5] ; Avocado
   [5] ; Banana
   [5] ; Pear
   ])

; Customers and the products they want to buy. However,
; the only store (Aldi) doesn't have enough products to fulfill
; anybody's order :(
;
; E.g. customer 0 wants to buy 6 bananas and 2 apples.
(def customers
  [{:id 0 :products [["Banana" 6] ["Apple" 2]]}
   {:id 1 :products [["Banana" 3] ["Apple" 9]]}
   {:id 2 :products [["Pear" 6]]}
   {:id 3 :products [["Apple" 1] ["Avocado" 4] ["Banana" 6] ["Pear" 2]]}
   {:id 4 :products [["Apple" 6] ["Apple" 2]]}
   {:id 5 :products [["Banana" 7]]}
   {:id 6 :products [["Avocado" 10] ["Apple" 1]]}
   {:id 7 :products [["Banana" 3] ["Apple" 5] ["Pear" 7]]}
   {:id 8 :products [["Pear" 1] ["Banana" 8]]}
   {:id 9 :products [["Apple" 6] ["Pear" 4] ["Avocado" 3]]}])

; Time in milliseconds between sales periods
(def TIME_BETWEEN_SALES 50)
; Time in milliseconds of sales period
(def TIME_OF_SALES 10)
