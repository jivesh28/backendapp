package com.smartexpense.service;

import com.smartexpense.model.Category;
import com.smartexpense.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    private static final List<Category> DEFAULT_CATEGORIES = List.of(
        Category.builder().name("Food").createdBy("SYSTEM")
            .keywords("swiggy,zomato,dominos,domino,mcdonalds,kfc,burger,pizza,freshMenu,box8,faasos,ubereats,biryani,restaurant").build(),
        Category.builder().name("Transport").createdBy("SYSTEM")
            .keywords("uber,ola,rapido,irctc,makemytrip,goibibo,bus,train,metro,cab,taxi,flight,indigo,airasia,spicejet").build(),
        Category.builder().name("Shopping").createdBy("SYSTEM")
            .keywords("amazon,flipkart,myntra,meesho,ajio,nykaa,snapdeal,shopify,ecommerce,bigbasket,grofers,blinkit").build(),
        Category.builder().name("Entertainment").createdBy("SYSTEM")
            .keywords("netflix,spotify,hotstar,bookmyshow,pvr,inox,prime,youtube,disney,zee,sonyliv,gaming").build(),
        Category.builder().name("Utilities").createdBy("SYSTEM")
            .keywords("electricity,rent,wifi,broadband,water,gas,jio,airtel,bsnl,vodafone,sbi,hdfc,bank,emi,loan").build(),
        Category.builder().name("Healthcare").createdBy("SYSTEM")
            .keywords("apollo,pharmacy,clinic,hospital,medicine,doctor,healthkart,netmeds,pharmeasy,1mg,lab,test").build(),
        Category.builder().name("Fuel").createdBy("SYSTEM")
            .keywords("petrol,diesel,bpcl,hpcl,iocl,shell,fuel,cng").build(),
        Category.builder().name("Groceries").createdBy("SYSTEM")
            .keywords("dmart,reliance,fresh,supermarket,vegetables,fruits,milk,dairy,bread,zepto,instamart").build()
    );

    @Override
    public void run(ApplicationArguments args) {
        long count = categoryRepository.count();
        if (count == 0) {
            categoryRepository.saveAll(DEFAULT_CATEGORIES);
            log.info("✅ Seeded {} default categories", DEFAULT_CATEGORIES.size());
        } else {
            log.info("ℹ️  Categories already seeded ({} found)", count);
        }
    }
}
