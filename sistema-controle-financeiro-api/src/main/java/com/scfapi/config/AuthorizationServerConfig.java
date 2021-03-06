package com.scfapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter{

	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Override
	public void configure(ClientDetailsServiceConfigurer clientsConfigure) throws Exception {
		
		//Configurações de tipos de escopos para cada tipo de cliente
		clientsConfigure.inMemory()
		.withClient("angular")
		.secret("$2a$10$RJQa37HkC4PWvZlg2qHq0uSd1OhmyZn8Q27mQlHb2zMnKQuMwb7F2")
		.scopes("read", "write")
		.authorizedGrantTypes("password", "refresh_token")
		.accessTokenValiditySeconds(3600)
		.refreshTokenValiditySeconds(3600 * 12);
//		para continuar adicionando novos escopos baseado nas regras de negocio
//		.and()
//		.withClient("")
//		.secret("")
//		.scopes("read", "write")
//		.authorizedGrantTypes("password", "refresh_token")
//		.accessTokenValiditySeconds(900)
//		.refreshTokenValiditySeconds(3600 * 12);
	}
	
	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpointsConfigure) throws Exception {
		
		endpointsConfigure
		.tokenStore(tokenStore())
		.accessTokenConverter(accessTokenConverter())
		.userDetailsService(this.userDetailsService)
		.authenticationManager(authenticationManager);
	}

	@Bean
	public JwtAccessTokenConverter accessTokenConverter() {
		
		JwtAccessTokenConverter accessTokenConverter = new JwtAccessTokenConverter();
		accessTokenConverter.setSigningKey("sfc");
		return accessTokenConverter;
	}

	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(accessTokenConverter());
	}
}
