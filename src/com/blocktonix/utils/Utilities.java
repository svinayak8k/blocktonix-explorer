package com.blocktonix.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Utilities
{
  public static final Logger logger = LoggerFactory.getLogger(Utilities.class);

  private static List<String> swapRoutersList = new ArrayList<String>();

  private static ObjectMapper mapper = new ObjectMapper();

  static
  {
    // sushi swap
    swapRoutersList.add("0xd9e1ce17f2641f24ae83637ab66a2cca9c378b9f");
    // uniswap
    swapRoutersList.add("0x7a250d5630b4cf539739df2c5dacb4c659f2488d");
  }

  public static EthAccounts getEthAccounts() throws InterruptedException, ExecutionException
  {
    EthAccounts result = new EthAccounts();
    result = Constants.web3.ethAccounts().sendAsync().get();
    return result;
  }

  public static EthGetBalance getEthBalance(String ethAddress) throws InterruptedException, ExecutionException
  {
    EthGetBalance result = new EthGetBalance();
    result = Constants.web3.ethGetBalance(ethAddress, DefaultBlockParameter.valueOf("latest")).sendAsync().get();
    return result;
  }

  public static void getEthValue()
  {}

  public static String getCoinsList()
  {
    String coingeckoAPI = "https://api.coingecko.com/api/v3/coins/list?include_platform=true";
    String responseValue = null;
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(coingeckoAPI).build();
    Response response;
    try
    {
      response = client.newCall(request).execute();
      responseValue = response.body().string();
    }
    catch (IOException e)
    {
      logger.error("IOException getting coins list from CoinGecko " + e.getLocalizedMessage());
    }
    return responseValue;
  }

  public static ObjectNode getEquivalents(String contractAddress, String coingeckoCoinId, String blockTime)
  {
    String dateComplete = blockTime.split("T")[0];
    String[] dateArray = dateComplete.split("-");
    String date = dateArray[2];
    String month = dateArray[1];
    String year = dateArray[0];
    // String coingeckoAPI = "https://api.coingecko.com/api/v3/coins/" + coingeckoCoinId +
    // "/history?date=01-01-2020&localization=false";
    String coingeckoAPI =
        "https://api.coingecko.com/api/v3/coins/" + coingeckoCoinId + "/history?date=" + date + "-" + month + "-" + year + "&localization=false";
    logger.info(coingeckoAPI);
    String responseValue = null;
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(coingeckoAPI).build();
    Response response;
    ObjectNode responseNode = null;
    try
    {
      response = client.newCall(request).execute();
      logger.debug(coingeckoAPI);
      responseValue = response.body().string();
      ObjectNode resultNode = (ObjectNode) mapper.readTree(responseValue);
      if (resultNode.has("market_data"))
      {
        String id = resultNode.get("id").asText();
        String ethValue = resultNode.get("market_data").get("current_price").get("eth").asText();
        BigDecimal ethBigDecimal = NumberUtils.createBigDecimal(ethValue);
        if (ethValue != null) ethValue = String.valueOf(ethBigDecimal.setScale(6, BigDecimal.ROUND_DOWN));
        String usdValue = resultNode.get("market_data").get("current_price").get("usd").asText();
        BigDecimal usdBigDecimal = NumberUtils.createBigDecimal(usdValue);
        if (usdValue != null) usdValue = String.valueOf(usdBigDecimal.setScale(6, BigDecimal.ROUND_DOWN));
        logger.info(" eth value = " + ethValue);
        logger.info(" usd value = " + NumberUtils.createBigDecimal(usdValue));
        responseNode = mapper.createObjectNode();
        responseNode.putPOJO("id", id);
        responseNode.putPOJO("usd", usdValue);
        responseNode.putPOJO("eth", ethValue);
      }
    }
    catch (IOException e)
    {
      logger.error("IOException getting Eth value from CoinGecko " + e.getLocalizedMessage());
    }
    catch (IndexOutOfBoundsException e)
    {
      logger.error("Error Rounding ETH value" + e.getLocalizedMessage());
    }
    return responseNode;
  }

  public static String getContractABI(String contractAddress)
  {
    String etherscanAPI =
        "https://api.etherscan.io/api?module=contract&action=getabi&address=" + contractAddress + "&apikey=" + "J5WK7PZUEN9IA3F8HTKY74FDVWD6IDQK7N";
    String responseValue = null;
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(etherscanAPI).build();
    Response response;
    try
    {
      response = client.newCall(request).execute();
      responseValue = response.body().string();
    }
    catch (IOException e)
    {
      logger.error("IOException getting Contract ABI from Etherscan " + e.getLocalizedMessage());
    }
    logger.info("Contract ABI from Etherscan for " + contractAddress);
    return responseValue;
  }

  public static List<String> get4ByteAPI(String hexSignature) throws IOException
  {
    String byteAPI = "https://www.4byte.directory/api/v1/signatures/?hex_signature=" + hexSignature;
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(byteAPI).build();
    Response response = client.newCall(request).execute();
    String responseOutput = response.body().string();
    JsonNode responseNode = mapper.readTree(responseOutput);
    JsonNode resultsNode = responseNode.get("results");
    Iterator<JsonNode> resultsIterator = resultsNode.iterator();
    List<String> methods = new ArrayList<>();
    while (resultsIterator.hasNext())
    {
      methods.add(resultsIterator.next().get("text_signature").asText());
    }
    return methods;
  }

  public static LinkedList<String> split64(String data)
  {
    LinkedList<String> strings = new LinkedList<>();
    int index = 0;
    while (index < data.length())
    {
      strings.add(data.substring(index, Math.min(index + 64, data.length())));
      index += 64;
    }

    return strings;
  }

  public static boolean isSwapRouter(String contractAddress)
  {
    if (swapRoutersList.contains(contractAddress)) return true;
    else
      return false;
  }

  public static Double truncateDouble(double value, int decimals)
  {
    value = value * Math.pow(10, decimals);
    value = Math.floor(value);
    value = value / Math.pow(10, decimals);

    return value;
  }
}


