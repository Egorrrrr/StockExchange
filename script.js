const IP = "http://localhost:3001";
var market;
var code = null;
var inst;

function makeOrder(e){
  var side = e ? "BUY" : "SELL" ;
  var price = document.getElementById("price").value
  var qty = document.getElementById("qty").value
  
  fetch(IP+"/new-order-single",{
    method: 'POST',
    body:JSON.stringify({code: code, side:side, price:price, qty:qty, instrument:inst})
  })
  .then((response) => {
    

  })
  .then((data) => {
    
  })

}

function cancelOrder(e){
  
  var data = e.target.parentNode;
  var id = data.childNodes[0].innerHTML.substring(1, data.childNodes[0].length)
  var instr = data.childNodes[1].innerHTML
  var side = data.childNodes[2].innerHTML  
  fetch(IP+"/order-cancel-request",{
    method: 'POST',
    body:JSON.stringify({code: code, side:side, instrument:instr, id:id})
  })
  .then((response) => {
    

  })
  .then((data) => {
    
  })

}



function login(){
   var name = document.getElementById("loginname").value;
   Cookies.setCookie("name", name);
   var pass = document.getElementById("pass").value;
   fetch(IP+"/login",{
    method: 'POST',
    body:JSON.stringify({username:name})
  })
  .then((response) => {
    window.location.pathname = '/index.html'

  })
  .then((data) => {
    
  })
  
    
}

var targetcolor;
function printOrderBook(e){
  if(targetcolor != null){
    targetcolor.style.color = "green"
  }
  targetcolor = e.target
  e.target.style.color = "lightgray";
  var isntname = document.getElementById("inst");
  isntname.innerHTML = "INS:"+ e.target.innerHTML;
  inst = e.target.innerHTML;
  var selltable = document.getElementById("selltab")
  var buytable = document.getElementById("buytab")
  selltable.innerHTML = "";
  buytable.innerHTML = "";
  
  for(const[keysellord, valsellord] of Object.entries(market.instruments[inst].sellOrders)){

    var order = document.createElement("div");
    order.className = "order"

    var id = document.createElement("div");
    id.className = "id"
    id.innerHTML = "#" + keysellord;

    var price = document.createElement("div");
    price.className = "price";
    price.innerHTML = "$" + valsellord.price;

    var qty = document.createElement("div");
    qty.className = "qty";
    qty.innerHTML = valsellord.qty;

    order.appendChild(id);
    order.appendChild(price);
    order.appendChild(qty);
    selltable.appendChild(order)
    
  }
  for(const[keybuyord, valbuyord] of Object.entries(market.instruments[inst].buyOrders)){
    
    var order = document.createElement("div");
    order.className = "order"

    var id = document.createElement("div");
    id.className = "id"
    id.innerHTML = "#" + keybuyord;

    var price = document.createElement("div");
    price.className =  price;
    price.innerHTML = "$" + valbuyord.price;

    var qty = document.createElement("div");
    qty.className = "qty";
    qty.innerHTML = valbuyord.qty;

    order.appendChild(id);
    order.appendChild(price);
    order.appendChild(qty);
    buytable.appendChild(order)
  }



}

function restoreTable(){
  var table = document.getElementById("tableone");
  table.innerHTML = "";

  var instheader = document.createElement("th");
  var sellheader = document.createElement("th");
  var buyheader = document.createElement("th");
  instheader.innerHTML = "INSTRUMENT"
  sellheader.innerHTML = "SELL"
  buyheader.innerHTML = "BUY"
  table.append(instheader)
  table.append(sellheader)  
  table.append(buyheader)  
}

if(document.location.toString().includes("index")){

    var flag = true
    if(flag){
      var msgEvent = new EventSource(IP + "/sse")

      msgEvent.addEventListener("connected", msg =>{
          code = msg.data;
          var name = Cookies.getCookie("name");
          var req = new XMLHttpRequest();
          req.open("POST", IP + "/verify");
          req.send(JSON.stringify({name:name, code:code}));
      });

      msgEvent.addEventListener("receiveTrades", msg =>{
        var yourorders = JSON.parse(msg.data)
        console.log(yourorders)
        var list = document.getElementById("myt")
        list.innerHTML = "";
        for (const [key, value] of Object.entries(yourorders)) {
          
          var myorder = document.createElement("div")
          myorder.className = "urorder"

          var id = document.createElement("div");
          id.className = "urid ur";
          id.innerHTML = "#" + key

          var urinst = document.createElement("div");
          urinst.className = "urins ur";
          urinst.innerHTML = value.instrument

          var urside = document.createElement("div");
          urside.className = "urside ur";
          urside.innerHTML = value.side;

          var urqty = document.createElement("div");
          urqty.className = "urqty ur";
          urqty.innerHTML = value.qty

          var urprice = document.createElement("div");
          urprice.className = "urprice ur";
          urprice.innerHTML = "$" + value.price

          

          myorder.appendChild(id);
          myorder.appendChild(urinst);
          myorder.appendChild(urside);
          myorder.appendChild(urqty);
          myorder.appendChild(urprice);
          list.appendChild(myorder)

        }
      });

      msgEvent.addEventListener("receiveOrders", msg =>{
        var yourorders = JSON.parse(msg.data)
        console.log(yourorders)
        var list = document.getElementById("myo")
        list.innerHTML = "";
        for (const [key, value] of Object.entries(yourorders)) {
          
          var myorder = document.createElement("div")
          myorder.className = "urorder"

          var id = document.createElement("div");
          id.className = "urid ur";
          id.innerHTML = "#" + key

          var urinst = document.createElement("div");
          urinst.className = "urins ur";
          urinst.innerHTML = value.instrument

          var urside = document.createElement("div");
          urside.className = "urside ur";
          urside.innerHTML = value.side;

          var urqty = document.createElement("div");
          urqty.className = "urqty ur";
          urqty.innerHTML = value.qty

          var urprice = document.createElement("div");
          urprice.className = "urprice ur";
          urprice.innerHTML = "$" + value.price

          var cancel = document.createElement("div");
          cancel.className = "cancel ur";
          cancel.innerHTML = "X"
          cancel.onclick = cancelOrder;

          myorder.appendChild(id);
          myorder.appendChild(urinst);
          myorder.appendChild(urside);
          myorder.appendChild(urqty);
          myorder.appendChild(urprice);
          myorder.appendChild(cancel);
          list.appendChild(myorder)

        }
      });


      msgEvent.addEventListener("marketSnapshot", msg =>{

        var marketSnapshot = JSON.parse(msg.data)
        market = marketSnapshot;
        restoreTable();
        for (const [key, value] of Object.entries(marketSnapshot.instruments)) {
          console.log(key, value);

          
          if(inst == key){
            var selltable = document.getElementById("selltab")
            var buytable = document.getElementById("buytab")
            selltable.innerHTML = "";
            buytable.innerHTML = "";
            
            for(const[keysellord, valsellord] of Object.entries(value.sellOrders)){

              var order = document.createElement("div");
              order.className = "order"

              var id = document.createElement("div");
              id.className = "id"
              id.innerHTML = "#" + keysellord;

              var price = document.createElement("div");
              price.className = "price";
              price.innerHTML = "$" + valsellord.price;

              var qty = document.createElement("div");
              qty.className = "qty";
              qty.innerHTML = valsellord.qty;

              order.appendChild(id);
              order.appendChild(price);
              order.appendChild(qty);
              selltable.appendChild(order)
              
            }

            for(const[keybuyord, valbuyord] of Object.entries(value.buyOrders)){
              
              var order = document.createElement("div");
              order.className = "order"

              var id = document.createElement("div");
              id.className = "id"
              id.innerHTML = "#" + keybuyord;

              var price = document.createElement("div");
              price.className = "price";
              price.innerHTML = "$" + valbuyord.price;

              var qty = document.createElement("div");
              qty.className = "qty";
              qty.innerHTML = valbuyord.qty;

              order.appendChild(id);
              order.appendChild(price);
              order.appendChild(qty);
              buytable.appendChild(order)
            }
          }
          var maxSell = bestSell(value);
          var minBuy = bestBuy(value);
          var table = document.getElementById("tableone");
          var tr = document.createElement("tr");
          var td = document.createElement("td");
          var tdsell = document.createElement("td");
          tdsell.innerHTML = maxSell;
          var tdbuy = document.createElement("td");
          tdbuy.innerHTML = minBuy;

          td.innerHTML = key;
          td.onclick = printOrderBook;
          tr.appendChild(td);
          tr.appendChild(tdsell);
          tr.appendChild(tdbuy)
          table.appendChild(tr);

        }        
    });
    flag= false
  }
}

function bestSell(instrument){

  var pricesArray = [];
  for (const [key, value] of Object.entries(instrument.sellOrders)) {
    pricesArray.push(value.price)

  }
  var result = Math.max(...pricesArray)
  return isFinite(result) ? result : "-" 

}
function bestBuy(instrument){

  var pricesArray = [];
  for (const [key, value] of Object.entries(instrument.buyOrders)) {
    pricesArray.push(value.price)

  }
  
  var result = Math.min(...pricesArray)
  return isFinite(result) ? result : "-" 

}




class Cookies {

    static setCookie(name, value, options = {}) {
      options = {
        path: '/',
        // при необходимости добавьте другие значения по умолчанию
        ...options
      };
    
      if (options.expires instanceof Date) {
        options.expires = options.expires.toUTCString()
      }
      let updatedCookie = encodeURIComponent(name) + "=" + encodeURIComponent(value)
    
      for (let optionKey in options) {
        updatedCookie += "; " + optionKey
        let optionValue = options[optionKey]
    
        if (optionValue != true) {
          updatedCookie += "=" + optionValue
        }
      }
      document.cookie = updatedCookie
    }
    
    static getCookie(name) {
      let matches = document.cookie.match(new RegExp(
        "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
      ))
      return matches ? decodeURIComponent(matches[1]) : undefined
    }
    
    static deleteAllCookies() {
      if(document.cookie != null){
      var cookies = document.cookie.split(";")
      for (var i = 0; i < cookies.length; i++) {
          var cookie = cookies[i]
          var eqPos = cookie.indexOf("=")
          var name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie
          document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT"
        }
    }
}
}
