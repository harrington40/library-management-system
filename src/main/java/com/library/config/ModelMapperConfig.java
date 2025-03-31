package com.library.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Add these imports (adjust package names as per your project structure)
import com.library.model.Loan;          // Entity class
import com.library.dto.LoanDTO;        // DTO class

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // Custom mappings for Loan → LoanDTO
        modelMapper.typeMap(Loan.class, LoanDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getBook().getId(), LoanDTO::setBookId);
            mapper.map(src -> src.getMember().getId(), LoanDTO::setMemberId);
        });

        return modelMapper;
    }
}